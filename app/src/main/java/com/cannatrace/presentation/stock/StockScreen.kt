package com.cannatrace.presentation.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cannatrace.domain.model.MovementType
import com.cannatrace.domain.model.StockEntry
import com.cannatrace.utils.DateUtils.toDisplayDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des stocks") },
                actions = {
                    IconButton(onClick = viewModel::loadAll) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un mouvement")
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
            // Alertes de stock faible
            if (state.lowStockBatchIds.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, tint = MaterialTheme.colorScheme.error, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "⚠️ ${state.lowStockBatchIds.size} lot(s) sous le seuil minimum",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Divergences non résolues
            if (state.pendingDivergences.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "🚨 ${state.pendingDivergences.size} divergence(s) de stock à signaler",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Obligation réglementaire : toute divergence doit être signalée à l'ANSM.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Mouvements récents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.entries.isEmpty()) {
                item {
                    Text(
                        "Aucun mouvement de stock enregistré.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.entries) { entry ->
                    StockEntryCard(entry = entry)
                }
            }
        }
    }

    // Dialog d'ajout de mouvement
    if (showAddDialog) {
        AddStockMovementDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { batchId, type, quantity, unit, reason ->
                viewModel.addStockEntry(batchId, type, quantity, unit, reason)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun StockEntryCard(entry: StockEntry) {
    val isIncoming = entry.movementType == MovementType.ENTREE || entry.movementType == MovementType.RETOUR
    val color = if (isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(entry.movementType.displayName, fontWeight = FontWeight.Medium)
                    Text(
                        "Lot : ${entry.batchId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.reason.take(50),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncoming) "+" else "-"}${entry.quantity} ${entry.unit}",
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    entry.timestamp.toDisplayDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddStockMovementDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MovementType, Double, String, String) -> Unit
) {
    var batchId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var reason by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MovementType.ENTREE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer un mouvement de stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = batchId,
                    onValueChange = { batchId = it },
                    label = { Text("ID / Numéro de lot") },
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text("Type de mouvement", style = MaterialTheme.typography.labelMedium)
                    MovementType.values().forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(type.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantité") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motif") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@TextButton
                    onConfirm(batchId, selectedType, qty, unit, reason)
                },
                enabled = batchId.isNotBlank() && quantity.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
