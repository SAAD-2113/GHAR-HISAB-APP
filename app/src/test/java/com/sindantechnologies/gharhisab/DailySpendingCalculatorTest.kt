package com.sindantechnologies.gharhisab

import com.sindantechnologies.gharhisab.domain.model.DailySpendingRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailySpendingCalculatorTest {

    @Test
    fun testDailySpendingRecommendationWithAmpleFunds() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 20) // 31 days in October, 20th means 12 days left (20..31 inclusive)

        // Available funds: Rs. 60,000 (6,000,000 paisa)
        // Upcoming planned commitments: Rs. 12,000 (1,200,000 paisa)
        // Protected reserve: Rs. 12,000 (1,200,000 paisa)
        // Spendable: 6,000,000 - 1,200,000 - 1,200,000 = 3,600,000 paisa (Rs. 36,000)
        // Remaining days: 31 - 20 + 1 = 12 days
        // Daily limit: 3,600,000 / 12 = 300,000 paisa (Rs. 3,000/day)

        val result = DailySpendingRecommendation.calculate(
            currentAvailableFundsPaisa = 6000000L,
            upcomingPlannedCommitmentsPaisa = 1200000L,
            protectedReservePaisa = 1200000L,
            calendar = cal
        )

        assertEquals(12, result.remainingDays)
        assertEquals(3600000L, result.spendableFundsPaisa)
        assertEquals(300000L, result.dailyLimitPaisa)
        assertFalse(result.isDeficit)
    }

    @Test
    fun testDailySpendingRecommendationWithDeficit() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 25)

        // Available funds: Rs. 5,000
        // Upcoming planned: Rs. 10,000
        // Protected reserve: Rs. 5,000
        // Spendable: 5,000 - 10,000 - 5,000 = -10,000 -> must clamp to 0

        val result = DailySpendingRecommendation.calculate(
            currentAvailableFundsPaisa = 500000L,
            upcomingPlannedCommitmentsPaisa = 1000000L,
            protectedReservePaisa = 500000L,
            calendar = cal
        )

        assertEquals(0L, result.dailyLimitPaisa)
        assertEquals(0L, result.spendableFundsPaisa)
        assertTrue(result.isDeficit)
        assertTrue(result.warningMessage != null)
    }

    @Test
    fun testDailySpendingRecommendationLastDayOfMonth() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 31) // Last day of month

        // Remaining days = 1 (today)
        val result = DailySpendingRecommendation.calculate(
            currentAvailableFundsPaisa = 100000L, // Rs. 1,000
            upcomingPlannedCommitmentsPaisa = 0L,
            protectedReservePaisa = 0L,
            calendar = cal
        )

        assertEquals(1, result.remainingDays)
        assertEquals(100000L, result.dailyLimitPaisa)
    }
}
