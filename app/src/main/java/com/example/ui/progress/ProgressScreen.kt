package com.example.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.WeightRecord
import com.example.data.database.DailyTrackerRecord
import com.example.data.database.PulseBreakRepository
import com.example.ui.components.ContributionGraph
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ProgressViewModel : ViewModel() {
    private val repository = PulseBreakApp.instance.repository
    
    val weightRecords: StateFlow<List<WeightRecord>> = repository.getAllWeightRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contributions: StateFlow<Map<String, Int>> = flow {
        val calendar = Calendar.getInstance()
        val dateKeys = mutableListOf<String>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        repeat(90) { // Approx 12-13 weeks
            dateKeys.add(sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        repository.getDailyTrackers(dateKeys).collect { records ->
            emit(records.associate { it.dateKey to it.workoutMinutes })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addWeight(weight: Float) {
        viewModelScope.launch {
            repository.addWeightRecord(weight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onNavigateBack: () -> Unit) {
    val viewModel = remember { ProgressViewModel() }
    val weights by viewModel.weightRecords.collectAsState()
    val activityData by viewModel.contributions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BODY & ACTIVITY", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Log Weight") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    ContributionGraph(
                        contributions = activityData,
                        modifier = Modifier.padding(16.dp),
                        weeksToShow = 12
                    )
                }
            }

            item {
                Text("WEIGHT HISTORY", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (weights.isEmpty()) {
                item { Text("No weight records yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(weights.size) { index ->
                    val record = weights[weights.size - 1 - index]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(record.dateKey, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${record.weightKg} kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            }
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("KG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var weightStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Weight") },
            text = {
                OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Weight (kg)") })
            },
            confirmButton = {
                TextButton(onClick = {
                    weightStr.toFloatOrNull()?.let { viewModel.addWeight(it) }
                    showDialog = false
                }) { Text("Save") }
            }
        )
    }
}
