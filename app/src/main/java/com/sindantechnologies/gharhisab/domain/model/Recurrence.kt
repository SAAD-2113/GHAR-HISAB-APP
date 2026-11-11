package com.sindantechnologies.gharhisab.domain.model

import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import java.time.Instant
import java.time.ZoneId

object Recurrence {
    fun anchorDay(plan: PlannedExpenseEntity): Int = plan.recurrenceAnchorDay.takeIf { it > 0 }
        ?: Instant.ofEpochMilli(plan.plannedDate).atZone(ZoneId.systemDefault()).dayOfMonth

    fun next(plan: PlannedExpenseEntity): Long? {
        val date = Instant.ofEpochMilli(plan.plannedDate).atZone(ZoneId.systemDefault())
        val interval = plan.recurrenceInterval.toLong().also { require(it in 1..120) }
        val next = when (plan.recurrence) {
            "WEEKLY" -> date.plusWeeks(interval)
            "MONTHLY" -> date.plusMonths(interval).let { it.withDayOfMonth(minOf(anchorDay(plan), it.toLocalDate().lengthOfMonth())) }
            "YEARLY" -> date.plusYears(interval).let { it.withDayOfMonth(minOf(anchorDay(plan), it.toLocalDate().lengthOfMonth())) }
            else -> return null
        }
        return next.toInstant().toEpochMilli()
    }

    /** Includes overdue unpaid occurrences; paid/cancelled plans never reserve money. */
    fun commitments(plans: List<PlannedExpenseEntity>, until: Long): Long = plans
        .filter { it.status == PlannedExpenseStatus.PLANNED }
        .fold(0L) { total, initial ->
            var sum = total
            var plan = initial.copy(recurrenceAnchorDay = anchorDay(initial))
            var count = 0
            while (plan.plannedDate <= until) {
                require(++count <= 10000) { "Recurrence range too large" }
                sum = Math.addExact(sum, plan.expectedAmount)
                val next = next(plan) ?: break
                plan = plan.copy(plannedDate = next)
            }
            sum
        }
}
