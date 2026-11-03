package com.cannatrace.presentation.culture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cannatrace.domain.model.CultureEntry
import com.cannatrace.domain.model.CultureEntryType
import com.cannatrace.utils.DateUtils.toDisplayDateTime

private val CultureEntryType.defaultUnit: String
    get() = when (this) {
        CultureEntryType.TEMPERATURE -> "°C"
        CultureEntryType.HUMIDITE -> "%"
        CultureEntryType.ARROSAGE -> "L"
        CultureEntryType.LUMIERE -> "h"
        CultureEntryType.NUTRIMENTS -> "mL"
        CultureEntryType.OBSERVATION -> ""
        CultureEntryType.TRAITEMENT -> "mL"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CultureJournalScreen(
    batchId: String,
    onNavigateBack: () -> Unit,
    viewModel: CultureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(batchId) { viewModel.loadForBatch(batchId) }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal de culture") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une entrée")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.entries.isEmpty() && !state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aucune entrée dans le journal de culture.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.entries) { entry ->
                    CultureEntryCard(entry = entry)
                }
            }
        }
    }

    if (showAddDialog) {
        AddCultureEntryDialog(
            batchId = batchId,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, value, unit, notes ->
                viewModel.addEntry(batchId, type, value, unit, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CultureEntryCard(entry: CultureEntry) {
    val icon = when (entry.type) {
        CultureEntryType.ARROSAGE -> Icons.Default.WaterDrop
        CultureEntryType.NUTRIMENTS -> Icons.Default.Science
        CultureEntryType.TEMPERATURE -> Icons.Default.Thermostat
        CultureEntryType.HUMIDITE -> Icons.Default.Cloud
        CultureEntryType.LUMIERE -> Icons.Default.LightMode
        CultureEntryType.OBSERVATION -> Icons.Default.Visibility
        CultureEntryType.TRAITEMENT -> Icons.Default.MedicalServices
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.type.displayName, fontWeight = FontWeight.Medium)
                Text(
                    "${entry.value} ${entry.unit}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.notes.isNotBlank()) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                entry.recordedAt.toDisplayDateTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddCultureEntryDialog(
    batchId: String,
    onDismiss: () -> Unit,
    onConfirm: (CultureEntryType, Double, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(CultureEntryType.TEMPERATURE) }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("°C") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle entrée journal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Type de mesure
                CultureEntryType.entries.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                unit = type.defaultUnit
                            }
                        )
                        Text(type.displayName)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Valeur") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toDoubleOrNull() ?: return@TextButton
                    onConfirm(selectedType, v, unit, notes)
                },
                enabled = value.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
