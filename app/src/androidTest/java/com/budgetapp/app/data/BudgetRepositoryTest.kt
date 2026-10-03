package com.budgetapp.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.budgetapp.app.data.backup.EncryptedBackupCodec
import com.budgetapp.app.data.csv.CsvParser
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.repository.BudgetRepository
import com.budgetapp.app.domain.CategorizationEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * Data-layer behaviour on a real database, driven the same way the screens drive it. These run on
 * a device (`./gradlew connectedDebugAndroidTest`) because Room + the encrypted backup codec both
 * need the Android runtime.
 */
@RunWith(AndroidJUnit4::class)
class BudgetRepositoryTest {
    private lateinit var database: BudgetDatabase
    private lateinit var repository: BudgetRepository

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, BudgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepository(
            database = database,
            csvParser = CsvParser(),
            categorizationEngine = CategorizationEngine(),
            profilePreferences = context.getSharedPreferences("test_profile_${System.nanoTime()}", 0),
            encryptedBackupCodec = EncryptedBackupCodec(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ---- import ----------------------------------------------------------------------------

    @Test
    fun reimportingTheSameFileSkipsEveryRow() = runBlocking {
        val csv = expenseCsv("2026-06-10;Coffee;-12,50")
        val first = importCsv(csv)
        val second = importCsv(csv)

        assertEquals(1, first.acceptedRows)
        assertEquals(0, second.acceptedRows)
        assertEquals(1, second.duplicateRows)
        assertEquals(1, repository.observeTransactions().first().size)
    }

    @Test
    fun incomeRowsAreImportedAndWaitInTheInbox() = runBlocking {
        val csv = expenseCsv("2026-06-10;Coffee;-12,50", "2026-06-11;SALARY;2500,00")
        val result = importCsv(csv)

        assertEquals(2, result.acceptedRows)
        assertEquals(1, result.incomeRows)
        val inbox = repository.observeInbox().first()
        val salary = inbox.single { it.description == "SALARY" }
        assertEquals(250000, salary.amountCents)
    }

    @Test
    fun oneUnparseableAmountFailsTheWholeImport() = runBlocking {
        val csv = expenseCsv("2026-06-10;Coffee;-12,50", "2026-06-11;Broken;not-a-number")

        val failure = runCatching { importCsv(csv) }

        assertTrue("import should fail loudly", failure.isFailure)
        assertEquals(0, repository.observeTransactions().first().size)
    }

    @Test
    fun aRuleIsCaseInsensitiveAndMatchesTheMerchantToo() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        repository.createRule(pattern = "CoFfEe", envelopeId = groceries)
        val today = LocalDate.now()

        importCsv(expenseCsv("$today;COFFEE BAR;-12,50"))

        assertEquals(1250, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    @Test
    fun historyImportedFromAnOlderStatementBecomesInvisible() = runBlocking {
        // Documents a real-world trap: a statement from a previous period imports fine, gets
        // assigned by a rule, and is then counted nowhere and listed nowhere, because balances and
        // the envelope's transaction list are both scoped to the current period.
        val groceries = repository.createEnvelope("Groceries", 40000)
        repository.createRule(pattern = "coffee", envelopeId = groceries)
        val oldDate = LocalDate.now().minusDays(45)

        val result = importCsv(expenseCsv("$oldDate;COFFEE BAR;-12,50"))

        assertEquals(1, result.acceptedRows)
        assertEquals(0, repository.observeInbox().first().size)
        val envelope = repository.observeEnvelopeBalances().first().single()
        assertEquals("spend is not counted", 0, envelope.spentCents)
        assertEquals("and the row is not listed anywhere either", 0, repository.observePeriodTransactionsForEnvelope(envelope.id).first().size)
        assertEquals(1, repository.observeTransactions().first().size)
    }

    @Test
    fun rowsWithoutAnEnvelopeLandInTheInbox() = runBlocking {
        importCsv(expenseCsv("2026-06-10;Coffee;-12,50", "2026-06-11;Books;-24,99"))

        assertEquals(2, repository.observeInbox().first().size)
        assertTrue(repository.observeEnvelopeBalances().first().isEmpty())
    }

    @Test
    fun aRuleAssignsMatchingImportsOnImport() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        repository.createRule(pattern = "coffee", envelopeId = groceries)
        val today = LocalDate.now()

        val result = importCsv(expenseCsv("$today;COFFEE BAR;-12,50"))

        assertEquals(1, result.acceptedRows)
        assertEquals(0, repository.observeInbox().first().size)
        val balance = repository.observeEnvelopeBalances().first().single()
        assertEquals(1250, balance.spentCents)
    }

    @Test
    fun splittingReplacesOneRowWithTwoThatSumToTheSameAmount() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        val household = repository.createEnvelope("Household", 20000)
        val original = addTransaction(groceries, 4218, LocalDate.now())

        repository.splitTransaction(original, household, 3000, groceries)

        val rows = repository.observeTransactions().first()
        assertEquals(2, rows.size)
        assertEquals(4218, rows.sumOf { -it.amountCents })
        assertEquals(3000, rows.first { it.envelopeId == household }.let { -it.amountCents })
        assertEquals(1218, rows.first { it.envelopeId == groceries }.let { -it.amountCents })
    }

    @Test
    fun aSplitNeedsTwoDifferentEnvelopes() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        val original = addTransaction(groceries, 1000, LocalDate.now())

        val failure = runCatching { repository.splitTransaction(original, groceries, 400, groceries) }

        assertTrue(failure.isFailure)
        assertEquals(1, repository.observeTransactions().first().size)
    }

    @Test
    fun aSplitCannotExceedTheOriginalAmount() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        val household = repository.createEnvelope("Household", 20000)
        val original = addTransaction(groceries, 1000, LocalDate.now())

        val failure = runCatching { repository.splitTransaction(original, household, 1000, groceries) }

        assertTrue(failure.isFailure)
        assertEquals(1, repository.observeTransactions().first().size)
    }

    // ---- envelopes -------------------------------------------------------------------------

    @Test
    fun archivingAnEnvelopeHidesItsTransactionsFromEveryTotal() = runBlocking {
        val envelope = repository.createEnvelope("Rent", 100000)
        addTransaction(envelope, 30000, LocalDate.now())

        repository.archiveEnvelope(envelope)

        assertTrue(repository.observeEnvelopeBalances().first().isEmpty())
        assertTrue("archived spend stays out of the inbox too", repository.observeInbox().first().isEmpty())
        assertEquals(1, repository.observeTransactions().first().size)
    }

    @Test
    fun anArchivedNameCanBeReused() = runBlocking {
        val first = repository.createEnvelope("Repairs", 5000)
        repository.archiveEnvelope(first)

        val second = repository.createEnvelope("Repairs", 7500)

        assertEquals(first, second)
        assertEquals(7500, repository.observeEnvelopeBalances().first().single().ceilingCents)
    }

    @Test
    fun duplicateActiveNamesAreRejected() = runBlocking {
        repository.createEnvelope("Rent", 100000)

        val failure = runCatching { repository.createEnvelope("Rent", 5000) }

        assertTrue(failure.isFailure)
    }

    @Test
    fun aZeroCeilingEnvelopeIsAllowed() = runBlocking {
        // The UI blocks it, the data layer does not: this documents what survives QA by hand.
        val id = repository.createEnvelope("Empty", 0)
        assertEquals(0, repository.observeEnvelopeBalances().first().single().ceilingCents)
        assertNotNull(id)
    }

    @Test
    fun aCreditFiledIntoAGoalCountsAsSaved() = runBlocking {
        val goal = repository.createEnvelope("Holiday", 200000, type = "goal")
        importCsv(expenseCsv("${LocalDate.now()};SALARY;2500,00"))
        val credit = repository.observeInbox().first().single()

        repository.assignTransactionToEnvelope(credit.id, goal)

        assertEquals(0, repository.observeInbox().first().size)
        assertEquals(250000, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    @Test
    fun aRefundFiledBackIntoASpendingEnvelopeStaysASpend() = runBlocking {
        // The sign follows the envelope, so a positive row filed into a spending envelope cannot
        // cancel out spend: it is stored as money leaving that envelope.
        val groceries = repository.createEnvelope("Groceries", 40000)
        importCsv(expenseCsv("${LocalDate.now()};REFUND;12,50"))
        val credit = repository.observeInbox().first().single()

        repository.assignTransactionToEnvelope(credit.id, groceries)

        assertEquals(1250, repository.observeEnvelopeBalances().first().single().spentCents)
        assertEquals(-1250, repository.observeTransactions().first().single().amountCents)
    }

    @Test
    fun assigningToASpendingEnvelopeCountsAsSpend() = runBlocking {
        val groceries = repository.createEnvelope("Groceries", 40000)
        importCsv(expenseCsv("${LocalDate.now()};Coffee;-12,50"))
        val inboxRow = repository.observeInbox().first().single()

        repository.assignTransactionToEnvelope(inboxRow.id, groceries)

        assertEquals(1250, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    // ---- period maths ----------------------------------------------------------------------

    @Test
    fun transactionsOnThePeriodEdgesCount() = runBlocking {
        val today = LocalDate.now()
        val periodStart = today.withDayOfMonth(1)
        val periodEnd = today.withDayOfMonth(today.lengthOfMonth())
        val envelope = repository.createEnvelope("Mixed", 100000)

        addTransaction(envelope, 100, periodStart)
        addTransaction(envelope, 200, periodEnd)
        addTransaction(envelope, 400, periodStart.minusDays(1))
        addTransaction(envelope, 800, periodEnd.plusDays(1))

        assertEquals(300, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    @Test
    fun shiftingTheViewedPeriodChangesWhatCounts() = runBlocking {
        val envelope = repository.createEnvelope("Groceries", 40000)
        addTransaction(envelope, 1200, LocalDate.now())
        addTransaction(envelope, 3400, LocalDate.now().minusMonths(1))

        assertEquals(1200, viewedSpend())

        repository.shiftPeriod(-1)
        assertEquals(3400, viewedSpend())

        repository.showCurrentPeriod()
        assertEquals(1200, viewedSpend())
    }

    @Test
    fun theViewedPeriodCannotMoveIntoTheFuture() = runBlocking {
        repository.shiftPeriod(3)
        assertEquals(0, repository.observePeriodOffsetMonths().value)
        repository.shiftPeriod(-1)
        repository.shiftPeriod(1)
        assertEquals(0, repository.observePeriodOffsetMonths().value)
    }

    private suspend fun viewedSpend(): Long =
        repository.observeEnvelopeBalances().first().single().spentCents

    @Test
    fun goalEnvelopesKeepAccumulatingAcrossPeriods() = runBlocking {
        val goal = repository.createEnvelope("Holiday", 200000, type = "goal")

        addTransaction(goal, 15000, LocalDate.now())
        addTransaction(goal, 25000, LocalDate.now().minusDays(45))

        assertEquals(40000, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    // ---- backup ----------------------------------------------------------------------------

    @Test
    fun backupRestoresAfterAWipe() = runBlocking {
        val envelope = repository.createEnvelope("Rent", 100000)
        addTransaction(envelope, 30000, LocalDate.now())
        repository.createRule(pattern = "coffee", envelopeId = envelope)
        val backup = repository.createEncryptedBackup().bytes

        repository.deleteAllLocalData()
        assertTrue(repository.observeEnvelopeBalances().first().isEmpty())

        val result = repository.restoreEncryptedBackup(backup)

        assertEquals(1, result.envelopeCount)
        assertEquals(1, result.transactionCount)
        assertEquals(1, result.ruleCount)
        assertEquals(30000, repository.observeEnvelopeBalances().first().single().spentCents)
    }

    @Test
    fun restoreRejectsAFileThatIsNotABackup() = runBlocking {
        val failure = runCatching { repository.restoreEncryptedBackup("date;amount\n2026-01-01;-1,00".toByteArray()) }

        assertTrue(failure.isFailure)
        assertEquals("This is not a Chap backup file.", failure.exceptionOrNull()?.message)
    }

    @Test
    fun restoreReplacesExistingDataRatherThanMerging() = runBlocking {
        val kept = repository.createEnvelope("Rent", 100000)
        addTransaction(kept, 30000, LocalDate.now())
        val backup = repository.createEncryptedBackup().bytes

        val extra = repository.createEnvelope("Fun", 5000)
        addTransaction(extra, 1000, LocalDate.now())

        repository.restoreEncryptedBackup(backup)

        val names = repository.observeEnvelopeBalances().first().map { it.name }
        assertEquals(listOf("Rent"), names)
    }

    @Test
    fun profileSettingsSurviveABackupRoundTrip() = runBlocking {
        repository.saveProfileSettings(
            repository.observeProfileSettings().value.copy(name = "Ugnius", currency = "PLN - Polish Złoty"),
        )
        val backup = repository.createEncryptedBackup().bytes

        repository.deleteAllLocalData()
        repository.restoreEncryptedBackup(backup)

        val restored = repository.observeProfileSettings().value
        assertEquals("Ugnius", restored.name)
        assertEquals("PLN - Polish Złoty", restored.currency)
    }

    @Test
    fun wipingClearsSettingsAndKeepsTheBackupKey() = runBlocking {
        val envelope = repository.createEnvelope("Rent", 100000)
        repository.saveProfileSettings(repository.observeProfileSettings().value.copy(name = "Before"))
        val backup = repository.createEncryptedBackup().bytes

        repository.deleteAllLocalData()

        assertEquals("Chap", repository.observeProfileSettings().value.name)
        assertNull(repository.observeEnvelopeBalances().first().firstOrNull())
        // the key that protects the backup must still be usable after a wipe
        assertEquals(1, repository.restoreEncryptedBackup(backup).envelopeCount)
    }

    // ---- helpers ---------------------------------------------------------------------------

    private suspend fun importCsv(csv: String) =
        repository.importMappedCsv(
            fileName = "test.csv",
            content = csv,
            hasHeader = true,
            dateIndex = 0,
            descriptionIndex = 1,
            amountIndex = 2,
        )

    private suspend fun addTransaction(envelopeId: Long, cents: Long, date: LocalDate): TransactionEntity {
        val id = repository.createCashTransaction(
            merchantName = "Test",
            amountCents = cents,
            envelopeId = envelopeId,
            postedAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            note = null,
        )
        return repository.observeTransactions().first().first { it.id == id }
    }

    private fun expenseCsv(vararg rows: String): String =
        (listOf("date;description;amount") + rows.toList()).joinToString("\n")
}
