package com.budgetapp.app.data.csv

import com.budgetapp.app.data.model.CsvTransactionDraft
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.Normalizer
import java.security.MessageDigest
import java.util.Locale

class CsvParser {
    fun parse(content: String): List<CsvTransactionDraft> {
        val inspection = inspect(content)
        val rows = inspection.rows
        val headerRowIndex = inspection.headerRowIndex

        if (rows.size <= headerRowIndex + 1) return emptyList()

        val headers = inspection.headers.map { it.normalized() }
        val dateIndex = headers.indexOfFirst { it in dateHeaders }
        val descriptionIndex = headers.indexOfFirst { it in descriptionHeaders }
        val merchantIndex = headers.indexOfFirst { it in merchantHeaders }
        val amountIndex = headers.indexOfFirst { it in amountHeaders }
        val debitCreditIndex = headers.indexOfFirst { it in debitCreditHeaders }
        val externalIdIndex = headers.indexOfFirst { it in externalIdHeaders }

        require(dateIndex >= 0) { "CSV must contain a date column" }
        require(descriptionIndex >= 0) { "CSV must contain a description column" }
        require(amountIndex >= 0) { "CSV must contain an amount column" }

        return parseMapped(
            content = content,
            hasHeader = true,
            headerRowIndex = headerRowIndex,
            dateIndex = dateIndex,
            descriptionIndex = descriptionIndex,
            amountIndex = amountIndex,
            merchantIndex = merchantIndex.takeIf { it >= 0 },
            debitCreditIndex = debitCreditIndex.takeIf { it >= 0 },
            externalIdIndex = externalIdIndex.takeIf { it >= 0 },
        )
    }

    fun inspect(content: String): CsvInspection {
        val delimiter = chooseDelimiter(content)
        val rows = content
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { parseLine(it, delimiter) }
            .toList()
        val headerRowIndex = rows.indices.maxByOrNull { rows[it].size } ?: 0
        val headers = rows.getOrNull(headerRowIndex).orEmpty()
        return CsvInspection(
            delimiter = delimiter,
            headers = headers,
            rows = rows,
            headerRowIndex = headerRowIndex,
        )
    }

    fun parseMapped(
        content: String,
        hasHeader: Boolean,
        headerRowIndex: Int = 0,
        dateIndex: Int,
        descriptionIndex: Int,
        amountIndex: Int,
        merchantIndex: Int? = null,
        debitCreditIndex: Int? = null,
        externalIdIndex: Int? = null,
    ): List<CsvTransactionDraft> {
        val rows = inspect(content).rows
        val dataRows = if (hasHeader) rows.drop(headerRowIndex + 1) else rows
        val sourceIdOccurrences = mutableMapOf<String, Int>()

        return dataRows.mapIndexedNotNull { index, row ->
            val date = row.getOrNull(dateIndex)?.trim().orEmpty()
            val description = row.getOrNull(descriptionIndex)?.trim().orEmpty()
            val amount = row.getOrNull(amountIndex)?.trim().orEmpty()
            if (date.isBlank() || description.isBlank() || amount.isBlank()) return@mapIndexedNotNull null

            val merchant = merchantIndex?.let { row.getOrNull(it) }?.trim()?.ifBlank { null }
            val postedAtMillis = parseDate(date)
            val amountCents = parseAmountCents(
                value = amount,
                debitCredit = debitCreditIndex?.let { row.getOrNull(it) }?.trim(),
            )
            val sourceExternalId = externalIdIndex
                ?.let { row.getOrNull(it) }
                ?.trim()
                ?.ifBlank { null }
            val sourceIdentityKey =
                sourceExternalId?.let {
                    listOf(it.trim().lowercase(Locale.US), postedAtMillis, amountCents).joinToString("|")
                }
            val sourceOccurrence =
                sourceIdentityKey?.let { key ->
                    sourceIdOccurrences.getOrDefault(key, 0).also {
                        sourceIdOccurrences[key] = it + 1
                    }
                } ?: 0
            CsvTransactionDraft(
                postedAtMillis = postedAtMillis,
                description = description,
                merchantName = merchant,
                amountCents = amountCents,
                externalId =
                    stableExternalId(
                        sourceExternalId = sourceExternalId,
                        sourceOccurrence = sourceOccurrence,
                        row = row,
                        rowIndex = index,
                        postedAtMillis = postedAtMillis,
                        description = description,
                        amountCents = amountCents,
                    ),
            )
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        values += current.toString()
        return values.dropLastWhile { it.isBlank() }
    }

    private fun chooseDelimiter(content: String): Char {
        val sample = content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        return if (sample.count { it == ';' } > sample.count { it == ',' }) ';' else ','
    }

    private fun parseDate(value: String): Long {
        val dateTime = dateTimeFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }
        if (dateTime != null) {
            return dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        val date = dateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        } ?: error("Unsupported CSV date: $value")

        return date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun parseAmountCents(
        value: String,
        debitCredit: String? = null,
    ): Long {
        val stripped = value
            .replace("\"", "")
            .replace("\u20ac", "")
            .replace("$", "")
            .replace("\u00a0", "")
            .replace(" ", "")
            .trim()
        val normalized = if (stripped.contains(',') && !stripped.contains('.')) {
            stripped.replace(',', '.')
        } else {
            stripped.replace(",", "")
        }
        val cents = (normalized.toBigDecimal() * 100.toBigDecimal()).toLong()
        val sign = when (debitCredit?.lowercase(Locale.US)) {
            "d", "debetas", "debit" -> -1L
            "k", "kreditas", "credit" -> 1L
            else -> if (cents < 0L) -1L else 1L
        }
        return kotlin.math.abs(cents) * sign
    }

    private fun stableExternalId(
        sourceExternalId: String?,
        sourceOccurrence: Int,
        row: List<String>,
        rowIndex: Int,
        postedAtMillis: Long,
        description: String,
        amountCents: Long,
    ): String {
        val normalizedSourceId = sourceExternalId?.trim()?.lowercase(Locale.US)
        if (normalizedSourceId != null && sourceOccurrence == 0) {
            return listOf("source", normalizedSourceId, postedAtMillis, amountCents)
                .joinToString("|")
                .hashCode()
                .toUInt()
                .toString(16)
        }
        if (normalizedSourceId != null) {
            return sha256(
                listOf(
                    "source-repeat-v2",
                    normalizedSourceId,
                    postedAtMillis,
                    amountCents,
                    normalizedRow(row),
                    sourceOccurrence,
                ).joinToString("|"),
            )
        }
        return listOf("row", rowIndex, postedAtMillis, description.trim().lowercase(Locale.US), amountCents)
            .joinToString("|")
            .hashCode()
            .toUInt()
            .toString(16)
    }

    private fun normalizedRow(row: List<String>): String =
        row.joinToString("\u001f") { value ->
            value.trim().lowercase(Locale.US).replace("\\s+".toRegex(), " ")
        }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val dateHeaders = setOf("date", "posted", "posted at", "transaction date", "data", "dokumento data")
        val descriptionHeaders = setOf("description", "details", "memo", "transaction", "mokejimo paskirtis", "moketojo arba gavejo pavadinimas")
        val merchantHeaders = setOf("merchant", "payee", "vendor", "moketojo arba gavejo pavadinimas")
        val amountHeaders = setOf("amount", "value", "sum", "suma", "suma saskaitos valiuta", "amount in account currency", "sum account currency")
        val debitCreditHeaders = setOf("debetas/kreditas", "debetas kreditas", "debit/credit", "debit credit", "debetas", "kreditas")
        val externalIdHeaders = setOf("dok nr.", "dok nr", "nuoroda", "reference", "transaction id", "transaction reference")
        val dateFormatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("d.M.yyyy", Locale.US),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US),
        )
        val dateTimeFormatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm", Locale.US),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.US),
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss", Locale.US),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.US),
        )
    }
}

private fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase(Locale.US), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")

data class CsvInspection(
    val delimiter: Char,
    val headers: List<String>,
    val rows: List<List<String>>,
    val headerRowIndex: Int,
)
