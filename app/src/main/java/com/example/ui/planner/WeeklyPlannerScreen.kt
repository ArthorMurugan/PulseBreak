package com.example.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.domain.model.PlannedExercise
import com.example.domain.model.WorkoutPlan
import com.example.ui.components.ExerciseMedia
import com.example.ui.components.exerciseAccentColor
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlannerViewModel : ViewModel() {
    private val repository = PulseBreakApp.instance.repository
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, PlannedExercise::class.java)
    private val adapter = moshi.adapter<List<PlannedExercise>>(listType)

    val workoutPlans: StateFlow<List<WorkoutPlan>> = repository.getAllWorkoutPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exerciseQuery = MutableStateFlow("")
    val exerciseQuery: StateFlow<String> = _exerciseQuery.asStateFlow()

    val filteredExercises: StateFlow<List<com.example.domain.model.Exercise>> = _exerciseQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            repository.searchExercises(query, limit = 50)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onExerciseQueryChange(query: String) {
        _exerciseQuery.value = query
    }

    fun updatePlan(
        dayOfWeek: Int, 
        name: String, 
        isRest: Boolean, 
        plannedExercises: List<PlannedExercise> = emptyList(),
        workSec: Int = 30,
        restSec: Int = 15,
        rounds: Int = 8
    ) {
        viewModelScope.launch {
            val isCardioRhythm = plannedExercises.isNotEmpty() && plannedExercises.all {
                it.exerciseId.startsWith("cardio_")
            }
            val exercisesWithSavedTiming = if (isCardioRhythm) {
                plannedExercises
            } else {
                plannedExercises.map { it.copy(workDurationSec = workSec, restDurationSec = restSec) }
            }
            val enrichedExercises = repository.enrichPlannedExercises(exercisesWithSavedTiming)
            val json = adapter.toJson(enrichedExercises)
            repository.saveWorkoutPlan(WorkoutPlan(
                dayOfWeek = dayOfWeek, 
                planName = name, 
                isRestDay = isRest, 
                plannedExercisesJson = json,
                defaultWorkSec = workSec,
                defaultRestSec = restSec,
                defaultRounds = rounds
            ))
        }
    }

    fun getPlannedExercises(json: String): List<PlannedExercise> {
        return try {
            if (json.isBlank()) emptyList() else adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlannerScreen(
    onNavigateBack: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val viewModel = remember { PlannerViewModel() }
    val currentPlans by viewModel.workoutPlans.collectAsState()
    val filteredExercises by viewModel.filteredExercises.collectAsState()
    val searchQuery by viewModel.exerciseQuery.collectAsState()
    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WEEKLY PLANNER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "PRESET PLANS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanPresetChip("PPL", "Push/Pull/Legs") {
                        val push = listOf(
                            PlannedExercise("0025", "Barbell Bench Press", 3, 10, 45, 60),
                            PlannedExercise("0426", "Dumbbell Overhead Press", 3, 12, 45, 60),
                            PlannedExercise("0001", "3/4 Sit-up", 3, 15, 30, 30)
                        )
                        val pull = listOf(
                            PlannedExercise("0032", "Barbell Deadlift", 3, 5, 45, 90),
                            PlannedExercise("1165", "Pull-up", 3, 8, 45, 60),
                            PlannedExercise("0031", "Barbell Curl", 3, 12, 30, 45)
                        )
                        val legs = listOf(
                            PlannedExercise("0043", "Barbell Full Squat", 3, 8, 60, 90),
                            PlannedExercise("0760", "Smith Leg Press", 3, 12, 45, 60),
                            PlannedExercise("0003", "Air Bike", 3, 20, 30, 30)
                        )
                        viewModel.updatePlan(2, "Push", false, push)
                        viewModel.updatePlan(3, "Pull", false, pull)
                        viewModel.updatePlan(4, "Legs", false, legs)
                        viewModel.updatePlan(5, "Rest Day", true, emptyList())
                        viewModel.updatePlan(6, "Push", false, push)
                        viewModel.updatePlan(7, "Pull", false, pull)
                        viewModel.updatePlan(1, "Legs", false, legs)
                    }
                    PlanPresetChip("Upper/Lower", "Strength Split") {
                        val upper = listOf(
                            PlannedExercise("0025", "Barbell Bench Press", 4, 8, 45, 90),
                            PlannedExercise("1165", "Pull-up", 4, 8, 45, 90),
                            PlannedExercise("0426", "Dumbbell Overhead Press", 3, 10, 45, 60)
                        )
                        val lower = listOf(
                            PlannedExercise("0043", "Barbell Full Squat", 4, 8, 60, 120),
                            PlannedExercise("0032", "Barbell Deadlift", 3, 5, 45, 120),
                            PlannedExercise("0760", "Smith Leg Press", 3, 12, 45, 90)
                        )
                        viewModel.updatePlan(2, "Upper Body", false, upper)
                        viewModel.updatePlan(3, "Lower Body", false, lower)
                        viewModel.updatePlan(4, "Rest Day", true, emptyList())
                        viewModel.updatePlan(5, "Upper Body", false, upper)
                        viewModel.updatePlan(6, "Lower Body", false, lower)
                        viewModel.updatePlan(7, "Rest Day", true, emptyList())
                        viewModel.updatePlan(1, "Rest Day", true, emptyList())
                    }
                    PlanPresetChip("Cardio", "HIIT Rhythm") {
                        val cardio = listOf(
                            PlannedExercise("cardio_fast", "Fast Run", 1, 1, 30, 0),
                            PlannedExercise("cardio_slow", "Slow Run", 1, 1, 30, 0),
                            PlannedExercise("cardio_walk", "Walking", 1, 1, 60, 0)
                        )
                        viewModel.updatePlan(3, "Cardio Rhythm", false, cardio, 30, 0, 15)
                        viewModel.updatePlan(5, "Cardio Rhythm", false, cardio, 30, 0, 15)
                        viewModel.updatePlan(7, "Cardio Rhythm", false, cardio, 30, 0, 15)
                    }
                }
            }

            item {
                Text(
                    "WEEKLY SCHEDULE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(days.indices.toList()) { index ->
                val dayOfWeek = index + 1
                val plan = currentPlans.find { it.dayOfWeek == dayOfWeek }
                DayPlanItem(
                    dayName = days[index],
                    plan = plan ?: WorkoutPlan(dayOfWeek, "FREE DAY", true),
                    plannedExercises = viewModel.getPlannedExercises(plan?.plannedExercisesJson ?: ""),
                    availableExercises = filteredExercises,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::onExerciseQueryChange,
                    onUpdate = { name, rest, plannedList, work, restTime, rnds -> 
                        viewModel.updatePlan(dayOfWeek, name, rest, plannedList, work, restTime, rnds) 
                    },
                    onStartWorkout = onStartWorkout
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PlanPresetChip(label: String, fullName: String, onClick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    FilterChip(
        selected = false,
        onClick = { showDialog = true },
        label = { Text(label) }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Apply $label?") },
            text = { Text("This will overwrite your current $fullName schedule.") },
            confirmButton = {
                TextButton(onClick = {
                    onClick()
                    showDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DayPlanItem(
    dayName: String,
    plan: WorkoutPlan,
    plannedExercises: List<PlannedExercise>,
    availableExercises: List<com.example.domain.model.Exercise>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUpdate: (String, Boolean, List<PlannedExercise>, Int, Int, Int) -> Unit,
    onStartWorkout: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val exerciseSummary = remember(plannedExercises) {
        plannedExercises.joinToString(", ") { "${it.name} (${it.sets}x${it.reps})" }
    }

    val isToday = remember {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        plan.dayOfWeek == today
    }
    val planAccent = exerciseAccentColor(plannedExercises.firstOrNull()?.bodyPart.orEmpty(), MaterialTheme.colorScheme)

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { showDialog = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isRestDay) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else planAccent.copy(alpha = 0.16f)
        ),
        border = if (isToday) BorderStroke(2.dp, planAccent) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dayName.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                    if (isToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("TODAY", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(text = if (plan.isRestDay) "REST DAY" else plan.planName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                if (!plan.isRestDay) {
                    if (exerciseSummary.isNotBlank()) {
                        Text(text = exerciseSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                    Text(
                        text = "${plan.defaultWorkSec}s Work / ${plan.defaultRestSec}s Rest • ${plan.defaultRounds} Rnds",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            plannedExercises.firstOrNull()?.let { exercise ->
                ExerciseMedia(
                    mediaUrl = exercise.gifUrl,
                    modifier = Modifier.size(64.dp),
                    backgroundColor = planAccent.copy(alpha = 0.16f)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            
            if (isToday && !plan.isRestDay) {
                IconButton(
                    onClick = onStartWorkout,
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                Icon(Icons.Default.Edit, contentDescription = null, tint = if (plan.isRestDay) Color.Gray else MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showDialog) {
        var text by remember { mutableStateOf(plan.planName) }
        var rest by remember { mutableStateOf(plan.isRestDay) }
    var workSecText by remember { mutableStateOf(plan.defaultWorkSec.toString()) }
    var restSecText by remember { mutableStateOf(plan.defaultRestSec.toString()) }
    var roundsText by remember { mutableStateOf(plan.defaultRounds.toString()) }
        
        val orchestrationList = remember { mutableStateListOf<PlannedExercise>().apply { addAll(plannedExercises) } }
        var showAddExercise by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit $dayName") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), 
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Plan Name") },
                        enabled = !rest,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rest = !rest }) {
                        Checkbox(checked = rest, onCheckedChange = { rest = it })
                        Text("Mark as Rest Day")
                    }
                    
                    if (!rest) {
                        Divider()
                        Text("Quick Actions", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    orchestrationList.clear()
                                    orchestrationList.add(PlannedExercise("cardio_fast", "Fast Run", 1, 1, 30, 0))
                                    orchestrationList.add(PlannedExercise("cardio_slow", "Slow Run", 1, 1, 30, 0))
                                    orchestrationList.add(PlannedExercise("cardio_walk", "Walking", 1, 1, 60, 0))
                                    text = "Cardio Rhythm"
                                    workSecText = "30"
                                    restSecText = "0"
                                    roundsText = "12"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Cardio Rhythm", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Divider()
                        Text("Interval Settings", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Work (s)", style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = workSecText,
                                    onValueChange = { workSecText = it.filter(Char::isDigit) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Rest (s)", style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = restSecText,
                                    onValueChange = { restSecText = it.filter(Char::isDigit) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Rounds", style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = roundsText,
                                    onValueChange = { roundsText = it.filter(Char::isDigit) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        Divider()
                        Text("Exercises:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        orchestrationList.forEachIndexed { i, pe ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(pe.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
                                        IconButton(onClick = { orchestrationList.removeAt(i) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Sets", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = pe.sets.toString(),
                                                onValueChange = { orchestrationList[i] = pe.copy(sets = it.toIntOrNull() ?: 1) },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("Reps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = pe.reps.toString(),
                                                onValueChange = { orchestrationList[i] = pe.copy(reps = it.toIntOrNull() ?: 0) },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { 
                                onSearchQueryChange("")
                                showAddExercise = true 
                            }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Exercise")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val workSec = workSecText.toIntOrNull()
                    val restSec = restSecText.toIntOrNull()
                    val rounds = roundsText.toIntOrNull()
                    if (workSec != null && workSec > 0 && restSec != null && restSec >= 0 && rounds != null && rounds > 0) {
                        onUpdate(text, rest, orchestrationList.toList(), workSec, restSec, rounds)
                        showDialog = false
                    }
                }) { Text("Save Plan", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
        
        if (showAddExercise) {
            ExercisePickerDialog(
                availableExercises = availableExercises,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onDismiss = { showAddExercise = false },
                onSelected = { ex ->
                    orchestrationList.add(PlannedExercise(ex.id, ex.name, gifUrl = ex.gifUrl, bodyPart = ex.bodyPart))
                    showAddExercise = false
                }
            )
        }
    }
}

@Composable
fun ExercisePickerDialog(
    availableExercises: List<com.example.domain.model.Exercise>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelected: (com.example.domain.model.Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name/muscle...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (availableExercises.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No exercises found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    }
                    items(availableExercises) { ex ->
                        val accent = exerciseAccentColor(ex.bodyPart, MaterialTheme.colorScheme)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelected(ex) },
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                ExerciseMedia(
                                    mediaUrl = ex.gifUrl,
                                    modifier = Modifier.size(58.dp),
                                    backgroundColor = accent.copy(alpha = 0.16f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = ex.name.uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    Text(text = ex.bodyPart.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
