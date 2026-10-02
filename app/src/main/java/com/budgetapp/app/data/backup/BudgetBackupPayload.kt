package com.budgetapp.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.budgetapp.app.data.model.EnvelopeType

@Serializable
data class BudgetBackupPayload(
    @SerialName("format_version") val formatVersion: Int = CurrentFormatVersion,
    @SerialName("database_version") val databaseVersion: Int,
    @SerialName("exported_at_millis") val exportedAtMillis: Long,
    val profile: BackupProfileSettings,
    val accounts: List<BackupAccount>,
    val envelopes: List<BackupEnvelope>,
    @SerialName("import_batches") val importBatches: List<BackupImportBatch>,
    val rules: List<BackupCategorizationRule>,
    val transactions: List<BackupTransaction>,
) {
    companion object {
        const val CurrentFormatVersion = 1
    }
}

@Serializable
data class BackupProfileSettings(
    val name: String,
    @SerialName("avatar_uri") val avatarUri: String? = null,
    val currency: String,
    @SerialName("date_format") val dateFormat: String,
    @SerialName("first_day_of_week") val firstDayOfWeek: String,
    @SerialName("month_start_day") val monthStartDay: String,
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    @SerialName("balance_cents") val balanceCents: Long,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("updated_at_millis") val updatedAtMillis: Long,
)

@Serializable
data class BackupEnvelope(
    val id: Long,
    val name: String,
    @SerialName("ceiling_cents") val ceilingCents: Long,
    @SerialName("color_token") val colorToken: String,
    @SerialName("icon_token") val iconToken: String = "wallet",
    val type: String = EnvelopeType.Variable,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("updated_at_millis") val updatedAtMillis: Long,
)

@Serializable
data class BackupImportBatch(
    val id: Long,
    @SerialName("file_name") val fileName: String,
    @SerialName("imported_at_millis") val importedAtMillis: Long,
    @SerialName("total_rows") val totalRows: Int,
    @SerialName("accepted_rows") val acceptedRows: Int,
    @SerialName("duplicate_rows") val duplicateRows: Int,
)

@Serializable
data class BackupCategorizationRule(
    val id: Long,
    val pattern: String,
    @SerialName("match_field") val matchField: String,
    @SerialName("envelope_id") val envelopeId: Long,
    val priority: Int,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
data class BackupTransaction(
    val id: Long,
    @SerialName("account_id") val accountId: Long,
    @SerialName("envelope_id") val envelopeId: Long?,
    @SerialName("import_batch_id") val importBatchId: Long?,
    @SerialName("posted_at_millis") val postedAtMillis: Long,
    val description: String,
    @SerialName("merchant_name") val merchantName: String?,
    @SerialName("amount_cents") val amountCents: Long,
    val note: String?,
    @SerialName("external_id") val externalId: String?,
    val status: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("updated_at_millis") val updatedAtMillis: Long,
)
