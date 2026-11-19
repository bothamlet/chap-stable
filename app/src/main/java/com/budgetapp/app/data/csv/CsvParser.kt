package com.budgetapp.app.data.csv

import com.budgetapp.app.data.model.CsvTransactionDraft
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.Normalizer
import java.security.MessageDigest
import java.util.Locale
import java.time.format.ResolverStyle
import com.budgetapp.app.domain.MaxAmountCents

class CsvParser {
    fun parse(content: String): List<CsvTransactionDraft> {
        val inspection = inspect(content)
        val rows = inspection.rows
        val headerRowIndex = inspection.headerRowIndex

        if (rows.size <= headerRowIndex + 1) return emptyList()

        val headers = inspection.headers.map { it.normalizedCsvHeader() }
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
        val rows = parseRecords(content.removePrefix("\uFEFF"), delimiter)
        val knownHeaders = dateHeaders + descriptionHeaders + merchantHeaders + amountHeaders +
            debitCreditHeaders + externalIdHeaders
        val headerRowIndex = rows.indices.maxWithOrNull(
            compareBy<Int> { index -> rows[index].count { it.normalizedCsvHeader() in knownHeaders } }
                .thenBy { rows[it].size },
        ) ?: 0
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
        require(listOf(dateIndex, descriptionIndex, amountIndex).all { it >= 0 }) { "Choose all required columns." }
        require(setOf(dateIndex, descriptionIndex, amountIndex).size == 3) { "Date, description and amount need different columns." }
        require(!hasHeader || headerRowIndex in rows.indices) { "Choose a valid header row." }
        val dataRows = if (hasHeader) rows.drop(headerRowIndex + 1) else rows
        val sourceIdOccurrences = mutableMapOf<String, Int>()

        return dataRows.mapIndexed { index, row ->
            val date = row.getOrNull(dateIndex)?.trim().orEmpty()
            val description = row.getOrNull(descriptionIndex)?.trim().orEmpty()
            val amount = row.getOrNull(amountIndex)?.trim().orEmpty()
            require(date.isNotBlank() && description.isNotBlank() && amount.isNotBlank()) {
                "CSV row ${index + if (hasHeader) headerRowIndex + 2 else 1} is missing a date, description or amount. Nothing was imported."
            }

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
            val sourceIdentityKey = listOf(
                sourceExternalId?.trim()?.lowercase(Locale.US).orEmpty(),
                postedAtMillis, amountCents, normalizedRow(row),
            ).joinToString("|")
            val sourceOccurrence =
                sourceIdentityKey.let { key ->
                    sourceIdOccurrences.getOrDefault(key, 0).also {
                        sourceIdOccurrences[key] = it + 1
                    }
                }
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
                        postedAtMillis = postedAtMillis,
                        description = description,
                        amountCents = amountCents,
                    ),
            )
        }
    }

    private fun parseRecords(content: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        fun finishRow() {
            values += current.toString()
            current.clear()
            if (values.any { it.isNotBlank() }) rows += values.toList()
            values = mutableListOf()
        }
        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && inQuotes && content.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    finishRow()
                    if (char == '\r' && content.getOrNull(index + 1) == '\n') index++
                }
                else -> current.append(char)
            }
            index++
        }
        require(!inQuotes) { "CSV contains an unclosed quoted field." }
        finishRow()
        return rows
    }

    private fun chooseDelimiter(content: String): Char {
        // Count separators outside quoted cells over several lines: bank files often begin
        // with a one-cell title, and merchant names may contain commas.
        val counts = mutableMapOf(',' to 0, ';' to 0, '\t' to 0)
        var inQuotes = false
        var lines = 0
        var index = 0
        while (index < content.length && lines < 20) {
            val char = content[index]
            if (char == '"') {
                if (inQuotes && content.getOrNull(index + 1) == '"') index++ else inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (char in counts) counts[char] = counts.getValue(char) + 1
                if (char == '\n') lines++
            }
            index++
        }
        return counts.maxBy { it.value }.key
    }

    private fun parseDate(value: String): Long {
        val dateTime = dateTimeFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }
        if (dateTime != null) {
            return dateTime
                .atZone(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }

        val date = dateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        } ?: error("Unsupported CSV date: $value")

        return date
            .atStartOfDay(java.time.ZoneOffset.UTC)
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
        val normalized = normalizeAmount(stripped)
        val cents = (normalized.toBigDecimal() * 100.toBigDecimal()).longValueExact()
        require(cents in -MaxAmountCents..MaxAmountCents) { "CSV amount is too large: $value" }
        val sign = when (debitCredit?.lowercase(Locale.US)) {
            "d", "debetas", "debit" -> -1L
            "k", "kreditas", "credit" -> 1L
            else -> if (cents < 0L) -1L else 1L
        }
        return kotlin.math.abs(cents) * sign
    }

    /**
     * Money in CSV files is written both as "1.234,56" (European) and "1,234.56" (US), and some
     * exports use a lone separator for thousands ("1,234"). Treating the last separator as the
     * decimal point when it is followed by one or two digits covers every case; a longer trailing
     * group is a thousands separator. Getting this wrong multiplies or divides the amount by 1000.
     */
    private fun normalizeAmount(value: String): String {
        require(value.matches(Regex("[+-]?[0-9]+(?:[.,][0-9]+)*"))) { "Invalid CSV amount: $value" }
        val digits = value.filter { it.isDigit() || it == '-' || it == '+' }
        val separatorIndex = maxOf(value.lastIndexOf(','), value.lastIndexOf('.'))
        if (separatorIndex == -1) {
            return digits
        }
        val fraction = value.substring(separatorIndex + 1).filter { it.isDigit() }
        if (fraction.length !in 1..2) {
            require(value.matches(Regex("[+-]?[1-9][0-9]{0,2}(?:,[0-9]{3})+|[+-]?[1-9][0-9]{0,2}(?:\\.[0-9]{3})+"))) {
                "Invalid CSV amount or decimal precision: $value"
            }
            return digits
        }
        val wholeText = value.substring(0, separatorIndex)
        require(wholeText.matches(Regex("[+-]?[0-9]+|[+-]?[1-9][0-9]{0,2}(?:,[0-9]{3})+|[+-]?[1-9][0-9]{0,2}(?:\\.[0-9]{3})+"))) { "Invalid CSV amount grouping: $value" }
        if (wholeText.contains(',') || wholeText.contains('.')) {
            require(!wholeText.contains(value[separatorIndex])) { "Invalid CSV amount grouping: $value" }
        }
        val whole = wholeText.filter { it.isDigit() || it == '-' || it == '+' }
        return "$whole.${fraction.padEnd(2, '0')}"
    }

    private fun stableExternalId(
        sourceExternalId: String?,
        sourceOccurrence: Int,
        row: List<String>,
        postedAtMillis: Long,
        description: String,
        amountCents: Long,
    ): String {
        val normalizedSourceId = sourceExternalId?.trim()?.lowercase(Locale.US)
        if (normalizedSourceId != null) {
            return sha256(
                listOf(
                    "source-v3",
                    normalizedSourceId,
                    postedAtMillis,
                    amountCents,
                    normalizedRow(row),
                    sourceOccurrence,
                ).joinToString("|"),
            )
        }
        return sha256(
            listOf("row-v3", postedAtMillis, description.trim().lowercase(Locale.US), amountCents, normalizedRow(row), sourceOccurrence)
                .joinToString("|"),
        )
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
            DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT),
        )
        val dateTimeFormatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu HH:mm", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu HH:mm:ss", Locale.US).withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss", Locale.US).withResolverStyle(ResolverStyle.STRICT),
        )
    }
}

internal fun String.normalizedCsvHeader(): String =
    Normalizer.normalize(trim().lowercase(Locale.US), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")

data class CsvInspection(
    val delimiter: Char,
    val headers: List<String>,
    val rows: List<List<String>>,
    val headerRowIndex: Int,
)
