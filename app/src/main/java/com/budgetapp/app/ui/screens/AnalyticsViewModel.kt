package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.repository.BudgetRepository
import com.budgetapp.app.domain.BudgetPeriod
import com.budgetapp.app.domain.budgetPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class AnalyticsState(
    val envelopes: List<EnvelopeBalance> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val profileSettings: ProfileSettings = ProfileSettings(),
) {
    val period: BudgetPeriod = profileSettings.budgetPeriod()
    val spendingEnvelopes: List<EnvelopeBalance> = envelopes.filter { it.type in EnvelopeType.SpendingTypes }
    private val spendingEnvelopeIds: Set<Long> = spendingEnvelopes.map { it.id }.toSet()
    val budgetCents: Long = spendingEnvelopes.sumOf { it.ceilingCents }
    val assignedExpenses: List<TransactionEntity> =
        transactions.filter { it.amountCents < 0 && it.envelopeId != null && it.envelopeId in spendingEnvelopeIds }
    val periodExpenses: List<TransactionEntity> =
        assignedExpenses.filter {
            val date = it.postedAtMillis.toAnalyticsDate()
            date >= period.start && date <= period.end
        }
    val spentCents: Long = periodExpenses.sumOf { -it.amountCents }
    val hasInsights: Boolean = assignedExpenses.isNotEmpty()
    val budgetPercent: Int = if (budgetCents <= 0L) 0 else ((spentCents * 100.0) / budgetCents).roundToInt()
    val forecastCents: Long = forecast(spentCents, period)
    val topEnvelope: EnvelopeBalance? = spendingEnvelopes.maxByOrNull { it.spentCents }
    val mostActiveDay: String =
        periodExpenses
            .groupingBy { it.postedAtMillis.toAnalyticsDate().dayOfWeek }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.getDisplayName(TextStyle.FULL, Locale.US)
            ?: "No day"
}

class AnalyticsViewModel(
    repository: BudgetRepository,
) : ViewModel() {
    val state: StateFlow<AnalyticsState> =
        combine(
            repository.observeEnvelopeBalances(),
            repository.observeTransactions(),
            repository.observeProfileSettings(),
        ) { envelopes, transactions, profileSettings ->
            AnalyticsState(envelopes = envelopes, transactions = transactions, profileSettings = profileSettings)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsState(),
        )
}

private fun Long.toAnalyticsDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun forecast(
    spentCents: Long,
    period: BudgetPeriod,
): Long =
    if (period.lengthInDays <= 0) {
        spentCents
    } else {
        (spentCents.toDouble() / dayOfPeriod(period) * period.lengthInDays).roundToInt().toLong()
    }

private fun dayOfPeriod(period: BudgetPeriod): Int =
    max(1, (LocalDate.now().toEpochDay() - period.start.toEpochDay() + 1).toInt().coerceAtMost(period.lengthInDays))
