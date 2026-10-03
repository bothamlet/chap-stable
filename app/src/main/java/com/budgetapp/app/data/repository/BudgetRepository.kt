package com.budgetapp.app.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import com.budgetapp.app.data.BudgetDatabase
import com.budgetapp.app.data.backup.BackupAccount
import com.budgetapp.app.data.backup.BackupCategorizationRule
import com.budgetapp.app.data.backup.BackupEnvelope
import com.budgetapp.app.data.backup.BackupImportBatch
import com.budgetapp.app.data.backup.BackupProfileSettings
import com.budgetapp.app.data.backup.BackupTransaction
import com.budgetapp.app.data.backup.BudgetBackupPayload
import com.budgetapp.app.data.backup.EncryptedBackupCodec
import com.budgetapp.app.data.csv.CsvParser
import com.budgetapp.app.data.entity.AccountEntity
import com.budgetapp.app.data.entity.CategorizationRuleEntity
import com.budgetapp.app.data.entity.EnvelopeEntity
import com.budgetapp.app.data.entity.ImportBatchEntity
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.ImportResult
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.model.RuleMatchField
import com.budgetapp.app.data.model.TransactionStatus
import com.budgetapp.app.data.model.toEnvelopeType
import com.budgetapp.app.domain.CategorizationEngine
import com.budgetapp.app.domain.BudgetPeriod
import java.time.LocalDate
import com.budgetapp.app.domain.budgetPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.util.Locale

data class BackupExport(
    val bytes: ByteArray,
    val exportedAtMillis: Long,
    val transactionCount: Int,
)

data class BackupRestoreResult(
    val exportedAtMillis: Long,
    val accountCount: Int,
    val envelopeCount: Int,
    val transactionCount: Int,
    val ruleCount: Int,
)

class BudgetRepository(
    private val database: BudgetDatabase,
    private val csvParser: CsvParser,
    private val categorizationEngine: CategorizationEngine,
    private val profilePreferences: SharedPreferences,
    private val encryptedBackupCodec: EncryptedBackupCodec,
) {
    private val budgetDao = database.budgetDao()
    private val transactionDao = database.transactionDao()
    private val ruleDao = database.categorizationRuleDao()
    private val importBatchDao = database.importBatchDao()
    private val profileSettings = MutableStateFlow(readProfileSettings())

    /**
     * How many months back the user is looking, 0 being the current period. Balances and the
     * envelope's transaction list both follow it, which is how the dashboard shows history: the
     * rows are still in the database, they are just outside the current period.
     */
    private val periodOffsetMonths = MutableStateFlow(0)

    fun observePeriodOffsetMonths(): StateFlow<Int> = periodOffsetMonths

    /** Moves the viewed period by [months]; the future is not interesting, so it clamps at 0. */
    fun shiftPeriod(months: Int) {
        periodOffsetMonths.value = (periodOffsetMonths.value + months).coerceIn(MaxPeriodOffsetMonths, 0)
    }

    fun showCurrentPeriod() {
        periodOffsetMonths.value = 0
    }

    /** The period the user is looking at, which is the current one unless they navigated back. */
    fun viewedPeriod(): BudgetPeriod =
        profileSettings.value.budgetPeriod(LocalDate.now().plusMonths(periodOffsetMonths.value.toLong()))

    private fun periodFlow(): Flow<BudgetPeriod> =
        combine(profileSettings, periodOffsetMonths) { settings, offset ->
            settings.budgetPeriod(LocalDate.now().plusMonths(offset.toLong()))
        }

    fun observeAccounts() = budgetDao.observeAccounts()

    fun observeActiveEnvelopes() = budgetDao.observeActiveEnvelopes()

    /**
     * Envelope balances for the current budget period. Goal envelopes accumulate savings across
     * periods, everything else resets when the period rolls over.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEnvelopeBalances(): Flow<List<EnvelopeBalance>> =
        periodFlow().flatMapLatest { period ->
            budgetDao.observeEnvelopeBalances(period.startMillis(), period.endExclusiveMillis())
        }

    fun observeTransactions() = transactionDao.observeTransactions()

    /** Transactions of one envelope inside the current budget period, newest first. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePeriodTransactionsForEnvelope(envelopeId: Long): Flow<List<TransactionEntity>> =
        periodFlow().flatMapLatest { period ->
            transactionDao.observeTransactionsForEnvelopeInPeriod(
                envelopeId = envelopeId,
                periodStartMillis = period.startMillis(),
                periodEndMillis = period.endExclusiveMillis(),
            )
        }

    fun observeInbox() = transactionDao.observeInbox()

    fun observeRules() = ruleDao.observeRules()

    fun observeProfileSettings(): StateFlow<ProfileSettings> = profileSettings

    fun saveProfileSettings(settings: ProfileSettings) {
        val normalized = settings.copy(
            name = settings.name.trim().ifBlank { "Chap" },
        )
        writeProfileSettings(normalized)
        profileSettings.value = normalized
    }

    fun getLastBackupAtMillis(): Long? =
        profilePreferences.getLong(LastBackupAtMillisKey, 0L).takeIf { it > 0L }

    fun recordBackupCreated(exportedAtMillis: Long) {
        profilePreferences.edit(commit = true) {
            putLong(LastBackupAtMillisKey, exportedAtMillis)
        }
    }

    suspend fun deleteAllLocalData() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            profilePreferences.edit(commit = true) {
                clear()
            }
            // The backup key is deliberately kept: deleting it would make every backup the user
            // already exported permanently unreadable, including the one they made just before
            // wiping to start over.
        }
        profileSettings.value = ProfileSettings()
    }

    suspend fun createEncryptedBackup(): BackupExport =
        withContext(Dispatchers.IO) {
            val exportedAtMillis = now()
            val payload = database.withTransaction {
                BudgetBackupPayload(
                    databaseVersion = DatabaseBackupVersion,
                    exportedAtMillis = exportedAtMillis,
                    profile = readProfileSettings().toBackup(),
                    accounts = budgetDao.getAccounts().map { it.toBackup() },
                    envelopes = budgetDao.getAllEnvelopes().map { it.toBackup() },
                    importBatches = importBatchDao.getImportBatches().map { it.toBackup() },
                    rules = ruleDao.getRules().map { it.toBackup() },
                    transactions = transactionDao.getTransactions().map { it.toBackup() },
                )
            }
            BackupExport(
                bytes = encryptedBackupCodec.encrypt(payload),
                exportedAtMillis = exportedAtMillis,
                transactionCount = payload.transactions.size,
            )
        }

    suspend fun restoreEncryptedBackup(bytes: ByteArray): BackupRestoreResult =
        withContext(Dispatchers.IO) {
            val payload = encryptedBackupCodec.decrypt(bytes)
            validateBackupPayload(payload)
            database.withTransaction {
                replaceLocalData(payload)
            }
            val restoredProfile = payload.profile.toProfileSettings()
            writeProfileSettings(
                settings = restoredProfile,
                lastBackupAtMillis = payload.exportedAtMillis,
                clearFirst = true,
            )
            profileSettings.value = restoredProfile
            BackupRestoreResult(
                exportedAtMillis = payload.exportedAtMillis,
                accountCount = payload.accounts.size,
                envelopeCount = payload.envelopes.size,
                transactionCount = payload.transactions.size,
                ruleCount = payload.rules.size,
            )
        }

    suspend fun createAccount(
        name: String,
        type: String,
        balanceCents: Long = 0,
    ): Long {
        val now = now()
        return budgetDao.insertAccount(
            AccountEntity(
                name = name.trim(),
                type = type.trim(),
                balanceCents = balanceCents,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    suspend fun createEnvelope(
        name: String,
        ceilingCents: Long,
        // The colour token survives from the ported data layer and the backup format, but the
        // mocks render every envelope in the accent colour and no screen reads it, so new
        // envelopes get the default instead of whatever the editor happened to hold.
        colorToken: String = DefaultColorToken,
        type: String = EnvelopeType.Variable,
        iconToken: String = "wallet",
    ): Long {
        val now = now()
        val trimmedName = name.trim()
        // The unique name index covers archived rows too, and archiving is one-way, so a name the
        // user has deleted is still taken. Reuse that row instead of failing the constraint: the
        // user is asking for an envelope with this name and these numbers.
        val archived = budgetDao.getAllEnvelopes().firstOrNull { it.name == trimmedName && it.isArchived }
        if (archived != null) {
            budgetDao.updateEnvelope(
                archived.copy(
                    ceilingCents = ceilingCents,
                    colorToken = colorToken,
                    iconToken = iconToken,
                    type = type,
                    isArchived = false,
                    updatedAtMillis = now,
                ),
            )
            return archived.id
        }
        val sortOrder = budgetDao.getActiveEnvelopes().size
        return budgetDao.insertEnvelope(
            EnvelopeEntity(
                name = name.trim(),
                ceilingCents = ceilingCents,
                colorToken = colorToken,
                iconToken = iconToken,
                type = type.toEnvelopeType(),
                sortOrder = sortOrder,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    suspend fun updateEnvelope(envelope: EnvelopeEntity) {
        budgetDao.updateEnvelope(envelope.copy(updatedAtMillis = now()))
    }

    suspend fun archiveEnvelope(id: Long) {
        budgetDao.archiveEnvelope(id, now())
    }

    suspend fun assignTransactionToEnvelope(
        transactionId: Long,
        envelopeId: Long,
    ) {
        val transaction = transactionDao.getById(transactionId)
        // The sign has to follow the target envelope: a credit filed into a goal has to count as
        // saved (goals only sum positive amounts), and a spend keeps its negative sign.
        val signedCents = transaction?.amountCents?.withEnvelopeSign(envelopeId) ?: return
        transactionDao.update(
            transaction.copy(
                envelopeId = envelopeId,
                amountCents = signedCents,
                status = TransactionStatus.Assigned,
                updatedAtMillis = now(),
            ),
        )
    }

    suspend fun createCashTransaction(
        merchantName: String,
        amountCents: Long,
        envelopeId: Long?,
        postedAtMillis: Long,
        note: String?,
        isIncome: Boolean = false,
    ): Long = database.withTransaction {
        val account = budgetDao.getAccounts().firstOrNull()
        val accountId = account?.id ?: createAccount(
            name = "Cash",
            type = "cash",
        )
        val now = now()
        transactionDao.insert(
            TransactionEntity(
                accountId = accountId,
                envelopeId = envelopeId,
                importBatchId = null,
                postedAtMillis = postedAtMillis,
                description = merchantName.trim(),
                merchantName = merchantName.trim(),
                amountCents = if (isIncome) {
                    kotlin.math.abs(amountCents)
                } else {
                    envelopeId?.let { amountCents.withEnvelopeSign(it) } ?: -kotlin.math.abs(amountCents)
                },
                note = note?.trim()?.ifBlank { null },
                status = if (envelopeId == null) TransactionStatus.Inbox else TransactionStatus.Assigned,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.update(transaction.copy(updatedAtMillis = now()))
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun createRule(
        pattern: String,
        envelopeId: Long,
        matchField: String = RuleMatchField.Any,
        priority: Int = 0,
    ): Long {
        return ruleDao.insert(
            CategorizationRuleEntity(
                pattern = pattern.trim(),
                matchField = matchField,
                envelopeId = envelopeId,
                priority = priority,
                createdAtMillis = now(),
            ),
        )
    }

    suspend fun updateRule(rule: CategorizationRuleEntity) {
        ruleDao.update(rule)
    }

    suspend fun deleteRule(id: Long) {
        ruleDao.deleteById(id)
    }

    suspend fun importMappedCsv(
        fileName: String,
        content: String,
        hasHeader: Boolean,
        headerRowIndex: Int = 0,
        dateIndex: Int,
        descriptionIndex: Int,
        amountIndex: Int,
        merchantIndex: Int? = null,
        debitCreditIndex: Int? = null,
        externalIdIndex: Int? = null,
        accountId: Long? = null,
    ): ImportResult = database.withTransaction {
        val targetAccountId = accountId ?: getOrCreateImportAccount()
        importCsvRows(
            fileName = fileName,
            accountId = targetAccountId,
            parsedRows = csvParser.parseMapped(
                content = content,
                hasHeader = hasHeader,
                headerRowIndex = headerRowIndex,
                dateIndex = dateIndex,
                descriptionIndex = descriptionIndex,
                amountIndex = amountIndex,
                merchantIndex = merchantIndex,
                debitCreditIndex = debitCreditIndex,
                externalIdIndex = externalIdIndex,
            ),
        )
    }

    private suspend fun importCsvRows(
        fileName: String,
        accountId: Long,
        parsedRows: List<com.budgetapp.app.data.model.CsvTransactionDraft>,
    ): ImportResult {
        // Credits are imported too. They wait in the inbox with a positive amount, where they can
        // be saved into a goal envelope — dropping them silently lost salaries and refunds.
        val incomeRows = parsedRows.count { it.amountCents > 0L }
        val knownExternalIds = transactionDao.getExternalIds().toSet()
        val newRows = parsedRows.filterNot { it.externalId in knownExternalIds }
        val duplicateRows = parsedRows.size - newRows.size
        val rules = ruleDao.getEnabledRules()
        val now = now()

        val importBatchId = importBatchDao.insert(
            ImportBatchEntity(
                fileName = fileName,
                importedAtMillis = now,
                totalRows = parsedRows.size,
                acceptedRows = newRows.size,
                duplicateRows = duplicateRows,
            ),
        )

        val transactions = newRows.map { draft ->
            // Rules are written against spend merchants, so credits stay unassigned for the inbox.
            val envelopeId = if (draft.amountCents > 0L) null else categorizationEngine.categorize(draft, rules)
            TransactionEntity(
                accountId = accountId,
                envelopeId = envelopeId,
                importBatchId = importBatchId,
                postedAtMillis = draft.postedAtMillis,
                description = draft.description,
                merchantName = draft.merchantName,
                amountCents = draft.amountCents,
                externalId = draft.externalId,
                status = if (envelopeId == null) TransactionStatus.Inbox else TransactionStatus.Assigned,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
        }
        val acceptedRows = transactionDao.insertCountingAccepted(transactions)

        return ImportResult(
            importBatchId = importBatchId,
            totalRows = parsedRows.size,
            acceptedRows = acceptedRows,
            duplicateRows = duplicateRows + (newRows.size - acceptedRows),
            incomeRows = incomeRows,
        )
    }

    private suspend fun getOrCreateImportAccount(): Long {
        val account = budgetDao.getAccounts().firstOrNull()
        return account?.id ?: createAccount(
            name = "Imported CSV",
            type = "bank",
        )
    }

    private fun now(): Long = System.currentTimeMillis()

    private suspend fun Long.withEnvelopeSign(envelopeId: Long): Long {
        val envelopeType = budgetDao.getAllEnvelopes()
            .firstOrNull { it.id == envelopeId }
            ?.type
            ?.toEnvelopeType()
            ?: EnvelopeType.Variable
        return if (envelopeType == EnvelopeType.Goal) {
            kotlin.math.abs(this)
        } else {
            -kotlin.math.abs(this)
        }
    }

    private suspend fun replaceLocalData(payload: BudgetBackupPayload) {
        transactionDao.deleteAll()
        ruleDao.deleteAll()
        importBatchDao.deleteAll()
        budgetDao.deleteAllEnvelopes()
        budgetDao.deleteAllAccounts()

        budgetDao.insertAccounts(payload.accounts.map { it.toEntity() })
        budgetDao.insertEnvelopes(payload.envelopes.map { it.toEntity() })
        importBatchDao.insertAll(payload.importBatches.map { it.toEntity() })
        ruleDao.insertAll(payload.rules.map { it.toEntity() })
        val insertedTransactions = transactionDao.insertAll(payload.transactions.map { it.toEntity() })
        check(insertedTransactions.count { it != -1L } == payload.transactions.size) {
            "Could not restore every transaction."
        }
    }

    private fun validateBackupPayload(payload: BudgetBackupPayload) {
        require(payload.formatVersion == BudgetBackupPayload.CurrentFormatVersion) {
            "Unsupported backup format."
        }
        require(payload.databaseVersion <= DatabaseBackupVersion) {
            "This backup was created by a newer app version."
        }
        require(payload.exportedAtMillis > 0L) {
            "This backup file is damaged."
        }

        validateIds("accounts", payload.accounts.map { it.id })
        validateIds("envelopes", payload.envelopes.map { it.id })
        validateIds("import batches", payload.importBatches.map { it.id })
        validateIds("rules", payload.rules.map { it.id })
        validateIds("transactions", payload.transactions.map { it.id })

        validateUniqueValues("envelope names", payload.envelopes.map { it.name.normalizedKey() })
        validateUniqueValues(
            "rules",
            payload.rules.map { "${it.pattern.normalizedKey()}:${it.matchField.normalizedKey()}" },
        )
        validateUniqueValues("external transaction IDs", payload.transactions.mapNotNull { it.externalId })

        val accountIds = payload.accounts.map { it.id }.toSet()
        val envelopeIds = payload.envelopes.map { it.id }.toSet()
        val importBatchIds = payload.importBatches.map { it.id }.toSet()
        payload.rules.forEach { rule ->
            require(rule.envelopeId in envelopeIds) {
                "This backup has a rule for a missing envelope."
            }
        }
        payload.transactions.forEach { transaction ->
            require(transaction.accountId in accountIds) {
                "This backup has a transaction for a missing account."
            }
            require(transaction.envelopeId == null || transaction.envelopeId in envelopeIds) {
                "This backup has a transaction for a missing envelope."
            }
            require(transaction.importBatchId == null || transaction.importBatchId in importBatchIds) {
                "This backup has a transaction for a missing import batch."
            }
        }
    }

    private fun validateIds(
        label: String,
        ids: List<Long>,
    ) {
        require(ids.all { it > 0L }) {
            "This backup has invalid $label."
        }
        require(ids.toSet().size == ids.size) {
            "This backup has duplicate $label."
        }
    }

    private fun validateUniqueValues(
        label: String,
        values: List<String>,
    ) {
        require(values.toSet().size == values.size) {
            "This backup has duplicate $label."
        }
    }

    private fun readProfileSettings(): ProfileSettings =
        ProfileSettings(
            name = profilePreferences.getString(ProfileNameKey, null) ?: ProfileSettings().name,
            avatarUri = profilePreferences.getString(ProfileAvatarUriKey, null),
            currency = profilePreferences.getString(ProfileCurrencyKey, null) ?: ProfileSettings().currency,
            dateFormat = profilePreferences.getString(ProfileDateFormatKey, null) ?: ProfileSettings().dateFormat,
            firstDayOfWeek = profilePreferences.getString(ProfileFirstDayKey, null) ?: ProfileSettings().firstDayOfWeek,
            monthStartDay = profilePreferences.getString(ProfileMonthStartKey, null) ?: ProfileSettings().monthStartDay,
        )

    private fun writeProfileSettings(
        settings: ProfileSettings,
        lastBackupAtMillis: Long? = getLastBackupAtMillis(),
        clearFirst: Boolean = false,
    ) {
        // apply(), not commit(true): this is reached straight from the "Save profile" click and a
        // synchronous fsync on the main thread stalls the frame for no benefit — the in-memory
        // StateFlow is updated by every caller straight after.
        profilePreferences.edit {
            if (clearFirst) {
                clear()
            }
            putString(ProfileNameKey, settings.name)
            putString(ProfileAvatarUriKey, settings.avatarUri)
            putString(ProfileCurrencyKey, settings.currency)
            putString(ProfileDateFormatKey, settings.dateFormat)
            putString(ProfileFirstDayKey, settings.firstDayOfWeek)
            putString(ProfileMonthStartKey, settings.monthStartDay)
            if (lastBackupAtMillis != null) {
                putLong(LastBackupAtMillisKey, lastBackupAtMillis)
            } else {
                remove(LastBackupAtMillisKey)
            }
        }
    }

    private fun ProfileSettings.toBackup(): BackupProfileSettings =
        BackupProfileSettings(
            name = name,
            avatarUri = avatarUri,
            currency = currency,
            dateFormat = dateFormat,
            firstDayOfWeek = firstDayOfWeek,
            monthStartDay = monthStartDay,
        )

    private fun BackupProfileSettings.toProfileSettings(): ProfileSettings =
        ProfileSettings(
            name = name.trim().ifBlank { ProfileSettings().name },
            avatarUri = avatarUri,
            currency = currency.ifBlank { ProfileSettings().currency },
            dateFormat = dateFormat.ifBlank { ProfileSettings().dateFormat },
            firstDayOfWeek = firstDayOfWeek.ifBlank { ProfileSettings().firstDayOfWeek },
            monthStartDay = monthStartDay.ifBlank { ProfileSettings().monthStartDay },
        )

    private fun AccountEntity.toBackup(): BackupAccount =
        BackupAccount(
            id = id,
            name = name,
            type = type,
            balanceCents = balanceCents,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun BackupAccount.toEntity(): AccountEntity =
        AccountEntity(
            id = id,
            name = name,
            type = type,
            balanceCents = balanceCents,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun EnvelopeEntity.toBackup(): BackupEnvelope =
        BackupEnvelope(
            id = id,
            name = name,
            ceilingCents = ceilingCents,
            colorToken = colorToken,
            iconToken = iconToken,
            type = type.toEnvelopeType(),
            sortOrder = sortOrder,
            isArchived = isArchived,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun BackupEnvelope.toEntity(): EnvelopeEntity =
        EnvelopeEntity(
            id = id,
            name = name,
            ceilingCents = ceilingCents,
            colorToken = colorToken,
            iconToken = iconToken,
            type = type.toEnvelopeType(),
            sortOrder = sortOrder,
            isArchived = isArchived,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun ImportBatchEntity.toBackup(): BackupImportBatch =
        BackupImportBatch(
            id = id,
            fileName = fileName,
            importedAtMillis = importedAtMillis,
            totalRows = totalRows,
            acceptedRows = acceptedRows,
            duplicateRows = duplicateRows,
        )

    private fun BackupImportBatch.toEntity(): ImportBatchEntity =
        ImportBatchEntity(
            id = id,
            fileName = fileName,
            importedAtMillis = importedAtMillis,
            totalRows = totalRows,
            acceptedRows = acceptedRows,
            duplicateRows = duplicateRows,
        )

    private fun CategorizationRuleEntity.toBackup(): BackupCategorizationRule =
        BackupCategorizationRule(
            id = id,
            pattern = pattern,
            matchField = matchField,
            envelopeId = envelopeId,
            priority = priority,
            isEnabled = isEnabled,
            createdAtMillis = createdAtMillis,
        )

    private fun BackupCategorizationRule.toEntity(): CategorizationRuleEntity =
        CategorizationRuleEntity(
            id = id,
            pattern = pattern,
            matchField = matchField,
            envelopeId = envelopeId,
            priority = priority,
            isEnabled = isEnabled,
            createdAtMillis = createdAtMillis,
        )

    private fun TransactionEntity.toBackup(): BackupTransaction =
        BackupTransaction(
            id = id,
            accountId = accountId,
            envelopeId = envelopeId,
            importBatchId = importBatchId,
            postedAtMillis = postedAtMillis,
            description = description,
            merchantName = merchantName,
            amountCents = amountCents,
            note = note,
            externalId = externalId,
            status = status,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun BackupTransaction.toEntity(): TransactionEntity =
        TransactionEntity(
            id = id,
            accountId = accountId,
            envelopeId = envelopeId,
            importBatchId = importBatchId,
            postedAtMillis = postedAtMillis,
            description = description,
            merchantName = merchantName,
            amountCents = amountCents,
            note = note,
            externalId = externalId,
            status = status,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )

    private fun String.normalizedKey(): String =
        trim().lowercase(Locale.US)

    private companion object {
        const val DatabaseBackupVersion = 3
        /** How far back the dashboard lets the user look. */
        const val MaxPeriodOffsetMonths = -120
        const val DefaultColorToken = "accent"
        const val ProfileNameKey = "profile_name"
        const val ProfileAvatarUriKey = "profile_avatar_uri"
        const val ProfileCurrencyKey = "profile_currency"
        const val ProfileDateFormatKey = "profile_date_format"
        const val ProfileFirstDayKey = "profile_first_day"
        const val ProfileMonthStartKey = "profile_month_start"
        const val LastBackupAtMillisKey = "last_backup_at_millis"
    }
}
