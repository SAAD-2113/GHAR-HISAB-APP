package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.domain.model.*
import com.sindantechnologies.gharhisab.data.local.room.entity.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*

class Version3FinanceTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 12)
    private fun time(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun expense(day: LocalDate, amount: Long, id: Long = day.toEpochDay()) = TransactionEntity(id, TransactionType.EXPENSE, amount, 1, null, time(day), null)
    private fun plan(date: LocalDate, amount: Long = 10000, recurrence: String = "NONE") = PlannedExpenseEntity(id = 1, description = "Bill", expectedAmount = amount, categoryId = 1, plannedDate = time(date), recurrence = recurrence)
    @Test fun noHistoryDoesNotInventDepletion() {
        val result = SpendingForecaster.calculate(emptyList(), emptyList(), 0, 0, false, today)
        assertNull(result.depletionDate); assertTrue(result.preliminary); assertEquals(0L, result.dailyLimit)
    }
    @Test fun oneDayIsPreliminaryAndDoesNotPredict() {
        val result = SpendingForecaster.calculate(listOf(expense(today, 100)), emptyList(), 1000, 0, true, today)
        assertNull(result.depletionDate); assertEquals(1, result.observedDays)
    }
    @Test fun stableSpendingAndMidMonthStart() {
        val transactions = (0L..9L).map { expense(today.minusDays(it), 100) }
        val result = SpendingForecaster.calculate(transactions, emptyList(), 1000, 0, true, today)
        assertEquals(100L, result.averageDaily); assertEquals(today.plusDays(9), result.depletionDate); assertFalse(result.preliminary)
    }
    @Test fun recentSpendingIsWeightedMore() {
        val tx = (0L..13L).map { expense(today.minusDays(it), if (it < 7) 300 else 100) }
        val result = SpendingForecaster.calculate(tx, emptyList(), 10000, 0, true, today)
        assertEquals(233L, result.averageDaily); assertTrue(result.increasing)
    }
    @Test fun overdueBillsAndReserveShowShortfall() {
        val result = SpendingForecaster.calculate(listOf(expense(today.minusDays(4), 100)), listOf(plan(today.minusDays(1), 1200)), 1000, 100, true, today)
        assertEquals(300L, result.shortfall); assertEquals(0L, result.dailyLimit); assertEquals(today, result.depletionDate)
    }
    @Test fun paidPlansAreNotReservedOrCountedInDailyRate() {
        val tx = listOf(expense(today.minusDays(4), 100), expense(today, 1000, 1))
        val result = SpendingForecaster.calculate(tx, listOf(plan(today, 1000).copy(status = PlannedExpenseStatus.PAID, actualTransactionId = 1)), 2000, 0, true, today)
        assertEquals(0L, result.commitments); assertEquals(20L, result.averageDaily)
    }
    @Test fun allCalendarMonthLengthsIncludeToday() {
        listOf(LocalDate.of(2024,2,1), LocalDate.of(2025,2,1), LocalDate.of(2026,4,1), LocalDate.of(2026,1,1)).forEach { date ->
            val result = SpendingForecaster.calculate(emptyList(), emptyList(), date.lengthOfMonth() * 100L, 0, true, date)
            assertEquals(100L, result.dailyLimit)
        }
    }
    @Test fun newIncomeAndBudgetCapAffectDailyLimit() {
        val first = SpendingForecaster.calculate(emptyList(), emptyList(), 1900, 0, true, today)
        val more = SpendingForecaster.calculate(emptyList(), emptyList(), 3800, 0, true, today)
        assertEquals(100L, first.dailyLimit); assertEquals(200L, more.dailyLimit)
        assertEquals(50L, SpendingForecaster.calculate(emptyList(), emptyList(), 3800, 0, true, today, budgetRemaining = 950).dailyLimit)
    }
    @Test fun monthEndRecurrencePreservesOriginalDay() {
        val january = plan(LocalDate.of(2024,1,31), recurrence = "MONTHLY")
        val february = january.copy(plannedDate = Recurrence.next(january)!!, recurrenceAnchorDay = Recurrence.anchorDay(january))
        assertEquals(LocalDate.of(2024,2,29), Instant.ofEpochMilli(february.plannedDate).atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2024,3,31), Instant.ofEpochMilli(Recurrence.next(february)!!).atZone(zone).toLocalDate())
    }
    @Test fun futureIncomeNotAssumedAndNegativeFundsDoNotRecommendSpending() {
        val result = SpendingForecaster.calculate(listOf(expense(today.plusDays(5), 1000).copy(type = TransactionType.INCOME)), emptyList(), -100, 0, false, today)
        assertEquals(0L, result.dailyLimit); assertNull(result.depletionDate)
    }
}
