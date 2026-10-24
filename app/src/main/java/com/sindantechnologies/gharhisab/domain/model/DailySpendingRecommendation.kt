package com.sindantechnologies.gharhisab.domain.model

import java.util.Calendar

/**
 * Encapsulates the dynamic daily safe-to-spend recommendation.
 *
 * Formula:
 * Spendable funds = Current available funds - Upcoming planned commitments - Protected reserve
 * Suggested daily limit = max(0, Spendable funds) ÷ Remaining days (including today)
 *
 * All amounts are Long in paisa.
 */
data class DailySpendingRecommendation(
    val dailyLimitPaisa: Long,
    val spendableFundsPaisa: Long,
    val currentAvailableFundsPaisa: Long,
    val upcomingPlannedCommitmentsPaisa: Long,
    val protectedReservePaisa: Long,
    val remainingDays: Int,
    val estimatedMonthEndBalancePaisa: Long,
    val isDeficit: Boolean,
    val warningMessage: String? = null,
    val hasSufficientData: Boolean = true
) {
    companion object {
        /**
         * Calculates safe-to-spend recommendation using exact integer arithmetic.
         */
        fun calculate(
            currentAvailableFundsPaisa: Long,
            upcomingPlannedCommitmentsPaisa: Long,
            protectedReservePaisa: Long,
            calendar: Calendar = Calendar.getInstance()
        ): DailySpendingRecommendation {
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Remaining days includes today
            val remainingDays = maxOf(1, maxDaysInMonth - currentDay + 1)

            val spendableFunds = currentAvailableFundsPaisa - upcomingPlannedCommitmentsPaisa - protectedReservePaisa
            val dailyLimit = if (spendableFunds > 0L) {
                spendableFunds / remainingDays
            } else {
                0L
            }

            val estimatedMonthEnd = currentAvailableFundsPaisa - upcomingPlannedCommitmentsPaisa
            val isDeficit = spendableFunds <= 0L

            val warning = when {
                currentAvailableFundsPaisa <= 0L ->
                    "Available funds are zero. Update your balance or log income to receive recommendations."
                isDeficit ->
                    "Upcoming commitments and reserve exceed your available funds. Consider adjusting planned expenses."
                dailyLimit < 10000L -> // Less than Rs. 100/day
                    "Your daily spendable limit is low. Prioritize essential household expenses."
                else -> null
            }

            return DailySpendingRecommendation(
                dailyLimitPaisa = dailyLimit,
                spendableFundsPaisa = maxOf(0L, spendableFunds),
                currentAvailableFundsPaisa = currentAvailableFundsPaisa,
                upcomingPlannedCommitmentsPaisa = upcomingPlannedCommitmentsPaisa,
                protectedReservePaisa = protectedReservePaisa,
                remainingDays = remainingDays,
                estimatedMonthEndBalancePaisa = estimatedMonthEnd,
                isDeficit = isDeficit,
                warningMessage = warning,
                hasSufficientData = currentAvailableFundsPaisa > 0L || upcomingPlannedCommitmentsPaisa > 0L
            )
        }
    }
}
