package com.example.mydayplanner

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/*
Composable settings panel. Use this in your Settings screen.
It persists reminder preferences in Room and reschedules alarms whenever saved.
Requires the SettingsDao + PlannerSettings entity merged into PlannerDatabase.
*/
@Composable
fun ReminderSettingsPanel() {
    val context = LocalContext.current
    val db = remember { PlannerDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val current by db.settingsDao().observeSettings().collectAsStateWithLifecycle(initialValue = null)
    var morningHour by remember(current) { mutableIntStateOf(current?.morningHour ?: 7) }
    var morningMinute by remember(current) { mutableIntStateOf(current?.morningMinute ?: 0) }
    var eveningHour by remember(current) { mutableIntStateOf(current?.eveningHour ?: 21) }
    var eveningMinute by remember(current) { mutableIntStateOf(current?.eveningMinute ?: 30) }
    var morningEnabled by remember(current) { mutableStateOf(current?.morningEnabled ?: true) }
    var eveningEnabled by remember(current) { mutableStateOf(current?.eveningEnabled ?: true) }
    var relaxedSunday by remember(current) { mutableStateOf(current?.relaxedSunday ?: true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Reminder settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Defaults: morning 07:00 · office/shop 09:00 · personal 19:00 · review 21:30")
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Morning planning")
                Text("%02d:%02d".format(morningHour, morningMinute), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = morningEnabled, onCheckedChange = { morningEnabled = it })
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Evening review")
                Text("%02d:%02d".format(eveningHour, eveningMinute), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = eveningEnabled, onCheckedChange = { eveningEnabled = it })
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Relaxed Sunday", Modifier.weight(1f))
            Switch(checked = relaxedSunday, onCheckedChange = { relaxedSunday = it })
        }
        Text("Time editing: wire these values to TimePickerDialog controls in the full settings screen.")
        Button(onClick = {
            scope.launch {
                val settings = PlannerSettings(
                    morningEnabled = morningEnabled,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    officeHour = 9,
                    officeMinute = 0,
                    personalHour = 19,
                    personalMinute = 0,
                    eveningEnabled = eveningEnabled,
                    eveningHour = eveningHour,
                    eveningMinute = eveningMinute,
                    workdaysCsv = "1,2,3,4,5,6",
                    relaxedSunday = relaxedSunday
                )
                db.settingsDao().save(settings)
                ReminderScheduler.scheduleDefaults(context, settings)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save reminder settings") }
    }
}
