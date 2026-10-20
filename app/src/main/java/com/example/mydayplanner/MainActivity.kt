package com.example.mydayplanner

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.createChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Starter defaults: 7 AM morning planning and 9 PM evening review.
        scheduleDailyReminder(8101, 7, 0, "Plan your day", "Choose your three priorities.")
        scheduleDailyReminder(8102, 21, 0, "Evening review", "Celebrate a win and plan tomorrow.")
        setContent { MaterialTheme { PlannerScreen() } }
    }

    private fun scheduleDailyReminder(id: Int, hour: Int, minute: Int, title: String, body: String) {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // MVP schedules the next occurrence. Production should schedule the next day after delivery.
        ReminderScheduler.schedule(this, id, title, body, c.timeInMillis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen() {
    val context = LocalContext.current
    val dao = remember { PlannerDatabase.get(context).taskDao() }
    val tasks by dao.observeTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var recurring by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(18) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val completed = tasks.count { it.completed }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Day", fontWeight = FontWeight.Bold)
                        Text("Small steps. Better days.", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add task")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Today") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Add, null) }, label = { Text("Plan") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(if (selectedTab == 0) "Your progress" else "Make a plan",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("$completed of ${tasks.size} tasks completed")
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Morning: 7:00 AM  •  Evening review: 9:00 PM",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Your tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Text("Add one important task, one thing for your health, and one thing you enjoy.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        ElevatedCard {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    scope.launch { dao.update(task.copy(completed = !task.completed)) }
                                }) {
                                    Icon(
                                        if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Toggle complete"
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${task.category} • ${SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(task.dueAtMillis))}" +
                                            if (task.recurring) " • Repeats" else "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add a task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") })
                    Text("Category")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Work", "Shop", "Health", "Personal").forEach { option ->
                            FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time: %02d:%02d".format(selectedHour, selectedMinute), Modifier.weight(1f))
                        TextButton(onClick = {
                            TimePickerDialog(context, { _, h, m -> selectedHour = h; selectedMinute = m },
                                selectedHour, selectedMinute, false).show()
                        }) { Text("Choose") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                        Text("Repeat daily (starter)")
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    val due = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour); set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                    }.timeInMillis
                    scope.launch {
                        val id = dao.insert(PlannerTask(
                            title = title.trim(), category = category, dueAtMillis = due, recurring = recurring
                        ))
                        ReminderScheduler.schedule(
                            context, (id % Int.MAX_VALUE).toInt(),
                            "Task reminder: ${title.trim()}", "It's time for your planned task.", due
                        )
                    }
                    title = ""; recurring = false; showAdd = false
                }) { Text("Save task") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
