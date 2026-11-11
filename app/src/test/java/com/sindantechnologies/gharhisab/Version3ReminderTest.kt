package com.sindantechnologies.gharhisab

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sindantechnologies.gharhisab.data.local.room.entity.PlannedExpenseEntity
import com.sindantechnologies.gharhisab.domain.model.PlannedExpenseStatus
import com.sindantechnologies.gharhisab.util.reminder.ReminderScheduler
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Version3ReminderTest {
    @Test fun changedDatesAndDisabledDeletedPlansCancelOldAlarms() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        val plan = PlannedExpenseEntity(id = 5, description = "Bill", expectedAmount = 100, categoryId = 1, plannedDate = System.currentTimeMillis() + 20L * 86400000)
        ReminderScheduler.reconcile(context, listOf(plan))
        assertEquals(3, alarms.scheduledAlarms.size)
        ReminderScheduler.reconcile(context, listOf(plan.copy(plannedDate = plan.plannedDate + 86400000, reminderDays = "7")))
        assertEquals(1, alarms.scheduledAlarms.size)
        ReminderScheduler.reconcile(context, listOf(plan.copy(remindersEnabled = false)))
        assertTrue(alarms.scheduledAlarms.isEmpty())
        ReminderScheduler.reconcile(context, listOf(plan))
        ReminderScheduler.reconcile(context, emptyList())
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun paidAndAlreadyDeliveredPlansDoNotNotifyAgain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        val plan = PlannedExpenseEntity(id = 8, description = "Bill", expectedAmount = 100, categoryId = 1, plannedDate = System.currentTimeMillis() + 86400000, reminderDays = "0")
        ReminderScheduler.markDelivered(context, plan, 0)
        ReminderScheduler.reconcile(context, listOf(plan))
        assertTrue(alarms.scheduledAlarms.isEmpty())
        ReminderScheduler.reconcile(context, listOf(plan.copy(status = PlannedExpenseStatus.PAID)))
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
}
