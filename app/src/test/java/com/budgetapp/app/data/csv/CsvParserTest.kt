package com.budgetapp.app.data.csv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFails

class CsvParserTest {
    private val parser = CsvParser()

    @Test
    fun transactionDatesAndDuplicateIdsSurviveADeviceTimeZoneChange() {
        val originalZone = java.util.TimeZone.getDefault()
        val csv = "date;description;amount\n2026-03-01;Coffee;-12,50"
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Minsk"))
            val first = parser.parse(csv).single()
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            assertEquals(first, parser.parse(csv).single())
        } finally {
            java.util.TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun quotedNewlinesAndTrailingEmptyCellsArePreserved() {
        val csv = "date;description;amount;merchant\r\n2026-06-10;\"Coffee\nlarge\";-12,50;\r\n"
        val inspection = parser.inspect(csv)
        assertEquals(2, inspection.rows.size)
        assertEquals(4, inspection.rows.last().size)
        assertEquals("Coffee\nlarge", parser.parse(csv).single().description)
    }

    @Test
    fun bomAndSingleCellPreambleDoNotHideTheHeader() {
        val csv = "\uFEFFStatement\ndate;description;amount\n2026-06-10;Coffee;-12,50"
        assertEquals(';', parser.inspect(csv).delimiter)
        assertEquals(-1250, parser.parse(csv).single().amountCents)
    }

    @Test
    fun missingRequiredValuesFailInsteadOfSilentlyDroppingRows() {
        assertFails { parser.parse("date;description;amount\n2026-06-10;;-12,50") }
    }

    @Test
    fun brokenQuotesFailInsteadOfChangingColumns() {
        assertFails { parser.inspect("date;description;amount\n2026-06-10;\"Coffee;-12,50") }
    }

    @Test
    fun impossibleDatesAreRejectedInsteadOfAdjusted() {
        assertFails { parser.parse("date;description;amount\n31.02.2026;Coffee;-12,50") }
    }

    @Test
    fun malformedAmountsCannotBeConvertedIntoPlausibleMoney() {
        listOf("1e3", "12abc34", "0.001", "1,23,45", "999999999999999999999", "1000000000").forEach {
            assertFails(it) { amountOf(it) }
        }
    }

    @Test
    fun rowIdentityWithoutBankIdsSurvivesReorderingAndOverlappingStatements() {
        val header = "date;description;amount"
        val coffee = "2026-06-10;Coffee;-12,50"
        val books = "2026-06-11;Books;-24,99"
        val first = parser.parse(listOf(header, coffee, books).joinToString("\n")).associateBy { it.description }
        val reordered = parser.parse(listOf(header, books, coffee).joinToString("\n")).associateBy { it.description }
        val overlap = parser.parse(listOf(header, books).joinToString("\n")).single()
        assertEquals(first, reordered)
        assertEquals(first.getValue("Books").externalId, overlap.externalId)
    }

    @Test
    fun identicalLegitimateRowsAreStillDistinctWithinAStatement() {
        val rows = parser.parse("date;description;amount\n2026-06-10;Coffee;-12,50\n2026-06-10;Coffee;-12,50")
        assertNotEquals(rows[0].externalId, rows[1].externalId)
    }

    @Test
    fun repeatedReferencesRemainStableWhenTheirSeparateTimeRowsMove() {
        val csv = repeatedReferenceWithSeparateTimeCsv()
        val reversed = (listOf(csv.lines().first()) + csv.lines().drop(1).reversed()).joinToString("\n")
        val first = parser.parse(csv).map { it.externalId }.toSet()
        assertEquals(first, parser.parse(reversed).map { it.externalId }.toSet())
    }

    @Test
    fun inspectUsesLongestRowAsHeader() {
        val inspection = parser.inspect(bankCsv())

        assertEquals(1, inspection.headerRowIndex)
        assertEquals(6, inspection.headers.size, "the trailing blank column is preserved")
        assertEquals("DOK NR.", inspection.headers.first())
    }

    @Test
    fun parseSupportsLithuanianBankDebitCreditColumns() {
        val rows = parser.parse(bankCsv())

        assertEquals(2, rows.size)
        assertEquals(-123456, rows[0].amountCents)
        assertEquals(250000, rows[1].amountCents)
        assertEquals("Grocery Store", rows[0].description)
        assertEquals("Salary", rows[1].description)
    }

    @Test
    fun sourceTransactionIdKeepsExternalIdStableWhenRowsMove() {
        val firstImport = parser.parse(bankCsv()).associateBy { it.description }
        val secondImport = parser.parse(bankCsv(reversed = true)).associateBy { it.description }

        assertEquals(firstImport.getValue("Grocery Store").externalId, secondImport.getValue("Grocery Store").externalId)
        assertEquals(firstImport.getValue("Salary").externalId, secondImport.getValue("Salary").externalId)
    }

    @Test
    fun repeatedBankReferenceDoesNotCollapseTransactionsThirtyMinutesApart() {
        val rows = parser.parse(repeatedReferenceCsv())

        assertEquals(2, rows.size)
        assertNotEquals(rows[0].externalId, rows[1].externalId)
        assertEquals(30 * 60 * 1000L, rows[1].postedAtMillis - rows[0].postedAtMillis)
    }

    @Test
    fun repeatedBankReferenceUsesOtherRowFieldsToDisambiguate() {
        val rows =
            parser.parseMapped(
                content = repeatedReferenceWithSeparateTimeCsv(),
                hasHeader = true,
                dateIndex = 1,
                descriptionIndex = 3,
                amountIndex = 2,
                externalIdIndex = 0,
            )

        assertEquals(2, rows.size)
        assertNotEquals(rows[0].externalId, rows[1].externalId)
        assertEquals(rows.map { it.externalId }, parser.parseMapped(
            content = repeatedReferenceWithSeparateTimeCsv(),
            hasHeader = true,
            dateIndex = 1,
            descriptionIndex = 3,
            amountIndex = 2,
            externalIdIndex = 0,
        ).map { it.externalId })
    }

    @Test
    fun parseReadsEuropeanAndUsAmountGrouping() {
        // "1.234,56" used to be read as 123 cents because the comma was stripped unconditionally.
        assertEquals(123456, amountOf("1.234,56"))
        assertEquals(123456, amountOf("1,234.56"))
        assertEquals(123456, amountOf("1234.56"))
        assertEquals(123456, amountOf("1 234,56"))
        // A lone separator followed by three digits is a thousands group, not a decimal point.
        assertEquals(123400, amountOf("1,234"))
        assertEquals(123400, amountOf("1.234"))
        assertEquals(1250, amountOf("12,5"))
        assertEquals(5, amountOf("0,05"))
    }

    @Test
    fun externalIdsAreSha256OnEveryPath() {
        val withSourceId = parser.parse(bankCsv()).first()
        val withoutSourceId =
            parser.parseMapped(
                content = "date;description;amount\n2026-06-10;Coffee;-12,50",
                hasHeader = true,
                dateIndex = 0,
                descriptionIndex = 1,
                amountIndex = 2,
            ).first()

        assertEquals(64, withSourceId.externalId.length)
        assertEquals(64, withoutSourceId.externalId.length)
        assertEquals(withSourceId.externalId, parser.parse(bankCsv()).first().externalId)
    }

    private fun amountOf(rawAmount: String): Long {
        val rows =
            parser.parseMapped(
                content = "date;description;amount\n2026-06-10;Coffee;$rawAmount",
                hasHeader = true,
                dateIndex = 0,
                descriptionIndex = 1,
                amountIndex = 2,
            )
        return rows.single().amountCents
    }

    private fun bankCsv(reversed: Boolean = false): String {
        val header = """
            "SASKAITOS ISRASAS";
            "DOK NR.";"DATA";"SUMA";"MOKĖJIMO PASKIRTIS";"DEBETAS/KREDITAS";
        """.trimIndent()
        val expense = """"A-100";"2026-06-10";"1 234,56";"Grocery Store";"D";"""
        val income = """"B-200";"2026-06-11";"2500,00";"Salary";"K";"""
        return if (reversed) {
            listOf(header, income, expense).joinToString("\n")
        } else {
            listOf(header, expense, income).joinToString("\n")
        }
    }

    private fun repeatedReferenceCsv(): String =
        """
        "DOK NR.";"DATA";"SUMA";"MOKEJIMO PASKIRTIS";
        "A-100";"2026-06-10 10:00";"-12,50";"Coffee Shop";
        "A-100";"2026-06-10 10:30";"-12,50";"Coffee Shop";
        """.trimIndent()

    private fun repeatedReferenceWithSeparateTimeCsv(): String =
        """
        "DOK NR.";"DATA";"SUMA";"MOKEJIMO PASKIRTIS";"LAIKAS";
        "A-100";"2026-06-10";"-12,50";"Coffee Shop";"10:00";
        "A-100";"2026-06-10";"-12,50";"Coffee Shop";"10:30";
        """.trimIndent()
}
