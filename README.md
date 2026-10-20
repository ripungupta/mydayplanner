# My Day Planner V2 — implementation patch

This patch adds configurable reminder settings and a recurrence/rescheduling approach for the Android starter.

## Defaults
- Morning planning: 07:00
- Office/shop start: 09:00 (shown as a routine anchor; it does not need a notification unless enabled)
- Personal time: 19:00
- Evening review: 21:30
- Routine: Monday–Saturday; relaxed Sunday

## Apply
1. Open the original `MyDayPlannerAndroid` project.
2. Replace `Reminders.kt` with the patch version.
3. Add `PlannerSettings.kt`.
4. Merge the `PlannerDatabase` changes shown in `PlannerDatabase_changes.kt` into your existing database file.
5. Increment Room database version and add a migration or clear app data during development. Do not ship a destructive migration to users with existing data.
6. Update `MainActivity` to load/save `PlannerSettings` and call `ReminderScheduler.scheduleDaily(...)` when settings change.
7. Add the exact-alarm access UX below before offering exact timing.

## Important
- Android does not guarantee exact delivery under all conditions. Exact alarms require special access on relevant Android versions; the app must check `AlarmManager.canScheduleExactAlarms()`.
- If exact access is unavailable, the patch uses an inexact alarm and the OS may delay it.
- Notifications require notification permission on Android 13+.
- User force-stop, revoked permissions, some OEM battery controls, and device-specific restrictions can prevent or delay notifications.
- The receiver reschedules the next daily occurrence after a daily reminder fires. The boot receiver restores daily alarms and task reminders from Room.
- For production, test daylight-saving/time-zone changes, reboot, app update, permission denial, exact-alarm access revocation, and recurring task date logic on real devices.
