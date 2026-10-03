package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.csv.CsvInspection
import com.budgetapp.app.data.csv.CsvParser
import com.budgetapp.app.data.csv.normalizedCsvHeader
import com.budgetapp.app.data.model.ImportResult
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

data class CsvImportState(
    val fileName: String? = null,
    val content: String = "",
    val inspection: CsvInspection? = null,
    val hasHeader: Boolean = true,
    val headerRowIndex: Int = 0,
    val dateIndex: Int = 0,
    val descriptionIndex: Int = 1,
    val amountIndex: Int = 2,
    val merchantIndex: Int? = null,
    val debitCreditIndex: Int? = null,
    val externalIdIndex: Int? = null,
    val isImporting: Boolean = false,
    val result: ImportResult? = null,
    val error: String? = null,
) {
    val isMapping: Boolean = inspection != null
    val isMappingComplete: Boolean = dateIndex >= 0 && descriptionIndex >= 0 && amountIndex >= 0

    /** Nothing in the header resembles a required column, so the file is probably not a CSV. */
    val hasNoRecognisableColumns: Boolean =
        dateIndex < 0 && descriptionIndex < 0 && amountIndex < 0
    val columns: List<String> = inspection?.headers.orEmpty()
    val rowCount: Int =
        if (hasHeader) {
            (inspection?.rows?.size ?: 0) - headerRowIndex - 1
        } else {
            inspection?.rows?.size ?: 0
        }
}

class CsvImportViewModel(
    private val repository: BudgetRepository,
    private val csvParser: CsvParser,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CsvImportState())
    val state: StateFlow<CsvImportState> = mutableState

    fun loadFile(
        fileName: String,
        content: String,
    ) {
        viewModelScope.launch {
            runCatching {
                // inspect() scans every row of a file that can be 10 MB, so it stays off the main
                // thread; the mapping screen appears when the state lands.
                val inspection = withContext(Dispatchers.Default) { csvParser.inspect(content) }
                val headers = inspection.headers.map { it.normalizedCsvHeader() }
                // Unrecognised columns stay unset (-1) instead of being guessed at column 0/1/2:
                // guessing meant a file without a date header failed with a parser error about the
                // first cell instead of asking which column holds the date.
                val dateIndex = headers.indexOfHeader("date", "posted", "transaction date", "data")
                val descriptionIndex = headers.indexOfHeader(
                    "description",
                    "details",
                    "memo",
                    "transaction",
                    "mokejimo paskirtis",
                    "moketojo arba gavejo pavadinimas",
                    "payee",
                    "merchant",
                )
                val amountIndex = headers.indexOfHeader(
                    "amount",
                    "value",
                    "sum",
                    "suma",
                    "suma saskaitos valiuta",
                )
                mutableState.value = CsvImportState(
                    fileName = fileName,
                    content = content,
                    inspection = inspection,
                    headerRowIndex = inspection.headerRowIndex,
                    dateIndex = dateIndex,
                    descriptionIndex = descriptionIndex,
                    amountIndex = amountIndex,
                    merchantIndex = headers.indexOfHeader(
                        "merchant",
                        "payee",
                        "vendor",
                        "mokejimo paskirtis",
                        "moketojo arba gavejo pavadinimas",
                    ).takeIf { it >= 0 },
                    debitCreditIndex = headers.indexOfHeader(
                        "debetas/kreditas",
                        "debetas kreditas",
                        "debit/credit",
                        "debit credit",
                        "debetas",
                        "kreditas",
                    ).takeIf { it >= 0 },
                    externalIdIndex = headers.indexOfHeader(
                        "dok nr.",
                        "dok nr",
                        "nuoroda",
                        "reference",
                        "transaction id",
                        "transaction reference",
                    ).takeIf { it >= 0 },
                )
            }.onFailure { error ->
                mutableState.update { it.copy(error = error.message ?: "Could not read CSV") }
            }
        }
    }

    fun showError(message: String) {
        mutableState.value = CsvImportState(error = message)
    }

    fun toggleHeader() {
        mutableState.update { it.copy(hasHeader = !it.hasHeader, result = null) }
    }

    fun setDateIndex(index: Int) {
        mutableState.update { it.copy(dateIndex = index, result = null) }
    }

    fun setDescriptionIndex(index: Int) {
        mutableState.update { it.copy(descriptionIndex = index, result = null) }
    }

    fun setAmountIndex(index: Int) {
        mutableState.update { it.copy(amountIndex = index, result = null) }
    }

    fun setMerchantIndex(index: Int) {
        mutableState.update { it.copy(merchantIndex = index, result = null) }
    }

    fun setDebitCreditIndex(index: Int) {
        mutableState.update { it.copy(debitCreditIndex = index, result = null) }
    }

    fun setExternalIdIndex(index: Int) {
        mutableState.update { it.copy(externalIdIndex = index, result = null) }
    }

    fun import() {
        val current = mutableState.value
        val fileName = current.fileName ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isImporting = true, error = null, result = null) }
            runCatching {
                repository.importMappedCsv(
                    fileName = fileName,
                    content = current.content,
                    hasHeader = current.hasHeader,
                    headerRowIndex = current.headerRowIndex,
                    dateIndex = current.dateIndex,
                    descriptionIndex = current.descriptionIndex,
                    amountIndex = current.amountIndex,
                    merchantIndex = current.merchantIndex,
                    debitCreditIndex = current.debitCreditIndex,
                    externalIdIndex = current.externalIdIndex,
                )
            }.onSuccess { result ->
                mutableState.update { it.copy(isImporting = false, result = result) }
            }.onFailure { error ->
                mutableState.update { it.copy(isImporting = false, error = error.message ?: "Import failed") }
            }
        }
    }
}

private fun List<String>.indexOfHeader(vararg candidates: String): Int =
    indexOfFirst { header -> candidates.any { candidate -> header == candidate || header.contains(candidate) } }

private fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase(Locale.US), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
