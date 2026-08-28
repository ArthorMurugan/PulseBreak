package com.example.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.NutritionRecord
import com.example.data.database.PulseBreakRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NutritionViewModel : ViewModel() {
    private val repository = PulseBreakApp.instance.repository
    val nutritionRecords: StateFlow<List<NutritionRecord>> = repository.getNutritionRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMeal(name: String) {
        viewModelScope.launch {
            repository.addNutritionRecord(NutritionRecord(dateKey = PulseBreakRepository.getTodayKey(), mealName = name))
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteNutritionRecord(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(onNavigateBack: () -> Unit) {
    val viewModel = remember { NutritionViewModel() }
    val records by viewModel.nutritionRecords.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MEAL LOG", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add Meal") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("TODAY'S MEALS", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { record ->
                    MealItem(record, onDelete = { viewModel.deleteMeal(record.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Meal") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Meal Name") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMeal(name)
                    showAddDialog = false
                }) { Text("Add") }
            }
        )
    }
}

@Composable
fun MealItem(record: NutritionRecord, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = record.mealName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
}
