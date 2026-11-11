package com.sindantechnologies.gharhisab.domain.model

import com.sindantechnologies.gharhisab.data.local.room.entity.*
import java.math.BigInteger
import java.time.*
import java.time.temporal.ChronoUnit

data class SpendingForecast(
    val dailyLimit: Long, val averageDaily: Long, val commitments: Long, val shortfall: Long,
    val depletionDate: LocalDate?, val observedDays: Int, val preliminary: Boolean,
    val increasing: Boolean, val balanceKnown: Boolean,
)

object SpendingForecaster {
    private fun exact(value: BigInteger): Long { require(value.bitLength() <= 63) { "Amount overflow" }; return value.toLong() }
    fun calculate(transactions: List<TransactionEntity>, plans: List<PlannedExpenseEntity>, balance: Long,
                  reserve: Long, balanceKnown: Boolean, today: LocalDate = LocalDate.now(),
                  zone: ZoneId = ZoneId.systemDefault(), budgetRemaining: Long? = null): SpendingForecast {
        val end = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val commitments = Recurrence.commitments(plans, end)
        val discretionary = balance.toBigInteger() - reserve.toBigInteger() - commitments.toBigInteger()
        val days = today.lengthOfMonth() - today.dayOfMonth + 1
        val available = if (budgetRemaining == null) discretionary else discretionary.min(budgetRemaining.toBigInteger())
        val limit = exact(available.max(BigInteger.ZERO) / days.toBigInteger())
        val dated = transactions.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() <= today }
        val first = dated.minOfOrNull { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val observed = if (first == null) 0 else minOf(14, ChronoUnit.DAYS.between(first, today).toInt() + 1)
        val paidIds = plans.mapNotNull { it.actualTransactionId }.toSet()
        val expenses = dated.filter { it.type == TransactionType.EXPENSE && it.id !in paidIds && it.plannedExpenseId == null }
        var recent = BigInteger.ZERO
        var previous = BigInteger.ZERO
        expenses.forEach { tx ->
            val age = ChronoUnit.DAYS.between(Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate(), today)
            if (age in 0..6) recent += tx.amount.toBigInteger()
            else if (age in 7..13) previous += tx.amount.toBigInteger()
        }
        val recentDays = minOf(7, observed)
        val previousDays = maxOf(0, observed - 7)
        val denominator = recentDays * 2 + previousDays
        val rate = if (denominator == 0) 0L else exact((recent * 2.toBigInteger() + previous) / denominator.toBigInteger())
        val increasing = previousDays > 0 && recent * previousDays.toBigInteger() > previous * recentDays.toBigInteger()
        var depletion: LocalDate? = null
        if (balanceKnown && observed >= 3) {
            val dues = mutableMapOf<LocalDate, BigInteger>()
            plans.filter { it.status == PlannedExpenseStatus.PLANNED }.forEach { initial ->
                var plan = initial.copy(recurrenceAnchorDay = Recurrence.anchorDay(initial))
                var count = 0
                while (count++ < 10000) {
                    val date = Instant.ofEpochMilli(plan.plannedDate).atZone(zone).toLocalDate().coerceAtLeast(today)
                    if (date > today.plusDays(365)) break
                    dues[date] = (dues[date] ?: BigInteger.ZERO) + plan.expectedAmount.toBigInteger()
                    plan = plan.copy(plannedDate = Recurrence.next(plan) ?: break)
                }
            }
            var remaining = balance.toBigInteger() - reserve.toBigInteger()
            for (offset in 0L..365L) {
                val day = today.plusDays(offset)
                remaining -= (dues[day] ?: BigInteger.ZERO) + rate.toBigInteger()
                if (remaining <= BigInteger.ZERO) { depletion = day; break }
            }
        }
        return SpendingForecast(limit, rate, commitments, exact((-discretionary).max(BigInteger.ZERO)), depletion,
            observed, observed < 10, increasing, balanceKnown)
    }
}
