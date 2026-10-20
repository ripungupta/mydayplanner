package com.example.mydayplanner

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "planner_reminders"
    const val MORNING_ID = 8101
    const val EVENING_ID = 8102
    const val PERSONAL_ID = 8103
    const val OFFICE_ID = 8104

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Planner reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily planning and task reminders" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun canUseExactAlarms(context: Context): Boolean {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    fun scheduleAt(context: Context, id: Int, title: String, body: String, atMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("kind", "one_shot")
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    fun cancel(context: Context, id: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending)
        pending.cancel()
    }

    fun nextDailyMillis(hour: Int, minute: Int, after: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= after) add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    fun scheduleDaily(context: Context, id: Int, title: String, body: String, hour: Int, minute: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("kind", "daily")
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = nextDailyMillis(hour, minute)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun scheduleDefaults(context: Context, settings: PlannerSettings) {
        cancel(context, MORNING_ID)
        cancel(context, EVENING_ID)
        cancel(context, PERSONAL_ID)
        cancel(context, OFFICE_ID)
        if (settings.morningEnabled) {
            scheduleDaily(context, MORNING_ID, "Plan your day",
                "Choose your three priorities.", settings.morningHour, settings.morningMinute)
        }
        if (settings.eveningEnabled) {
            scheduleDaily(context, EVENING_ID, "Evening review",
                "Celebrate a win and prepare tomorrow.", settings.eveningHour, settings.eveningMinute)
        }
        // Office/personal times are routine anchors by default, not notifications.
        // Enable these if the user explicitly asks for extra prompts:
        // scheduleDaily(context, PERSONAL_ID, "Personal time", "Take time for yourself.",
        //     settings.personalHour, settings.personalMinute)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createChannel(context)
        val id = intent.getIntExtra("id", 100)
        val title = intent.getStringExtra("title") ?: "My Day Planner"
        val body = intent.getStringExtra("body") ?: "Time to check your plan."

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission denied: notification cannot be shown.
        }

        // Daily reminders schedule their next local-time occurrence after firing.
        if (intent.getStringExtra("kind") == "daily") {
            val hour = intent.getIntExtra("hour", 7)
            val minute = intent.getIntExtra("minute", 0)
            ReminderScheduler.scheduleDaily(context, id, title, body, hour, minute)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.TIME_SET" &&
            intent.action != "android.intent.action.TIMEZONE_CHANGED") return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PlannerDatabase.get(context)
                val settings = db.settingsDao().getSettings() ?: PlannerSettings()
                ReminderScheduler.scheduleDefaults(context, settings)

                // Rehydrate non-completed task reminders. In production, also store a stable
                // notification ID on each task and handle recurring weekdays explicitly.
                db.taskDao().recurringTasks().forEach { task ->
                    if (!task.completed && task.dueAtMillis > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleAt(
                            context, task.id.toInt(),
                            "Task reminder: ${task.title}", "It's time for your planned task.",
                            task.dueAtMillis
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
