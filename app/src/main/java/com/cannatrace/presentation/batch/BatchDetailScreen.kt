package com.cannatrace.presentation.batch

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
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.CorrectionNote
import com.cannatrace.utils.DateUtils.toDisplayDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(
    batchId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCulture: (String) -> Unit,
    currentUserId: String = "user-001",
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(batchId) { viewModel.loadBatchDetail(batchId) }

    var transitionTarget by remember { mutableStateOf<BatchStatus?>(null) }
    var transitionReason by remember { mutableStateOf("") }
    var correctionJustification by remember { mutableStateOf("") }
    var correctionType by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.batch?.batchNumber ?: "Détail lot") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.batch?.isClosed == false) {
                        IconButton(onClick = viewModel::showTransitionDialog) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Changer le statut")
                        }
                    }
                    IconButton(onClick = viewModel::showCorrectionNoteDialog) {
                        Icon(Icons.Default.EditNote, contentDescription = "Note de correction")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val batch = state.batch ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Lot introuvable")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statut et badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatchStatusChip(status = batch.status)
                    if (batch.isClosed) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("LOT CLÔTURÉ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Informations principales
            item {
                InfoCard(title = "Informations du lot") {
                    InfoRow("Numéro", batch.batchNumber)
                    InfoRow("Souche", batch.strainName)
                    InfoRow("Poids", "${batch.weight} g")
                    InfoRow("THC", "${batch.thcContent} %")
                    InfoRow("CBD", "${batch.cbdContent} %")
                    InfoRow("Créé le", batch.createdAt.toDisplayDateTime())
                    InfoRow("Modifié le", batch.updatedAt.toDisplayDateTime())
                    InfoRow("Propriétaire", batch.ownerId)
                    InfoRow("Localisation", batch.locationId)
                }
            }

            // Hash de la blockchain légère
            item {
                InfoCard(title = "Intégrité (Blockchain légère)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (batch.hashChain.isNotBlank())
                                batch.hashChain.take(24) + "..."
                            else "Non calculé",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Notes
            if (batch.notes.isNotBlank()) {
                item {
                    InfoCard(title = "Notes") {
                        Text(
                            text = batch.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Notes de correction (lot clôturé)
            if (state.correctionNotes.isNotEmpty()) {
                item {
                    Text(
                        "Notes de correction (${state.correctionNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.correctionNotes) { note ->
                    CorrectionNoteCard(note = note)
                }
            }

            // Action : Journal de culture
            item {
                OutlinedButton(
                    onClick = { onNavigateToCulture(batchId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocalFlorist, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Voir le journal de culture")
                }
            }
        }

        // Messages d'erreur / succès
        state.error?.let { error ->
            LaunchedEffect(error) {
                // TODO: Snackbar
                viewModel.clearMessages()
            }
        }
    }

    // Dialog : Transition de statut
    if (state.showTransitionDialog) {
        val allowedStatuses = TransitionBatchStatusUseCase_companion_valid(batch = state.batch)

        AlertDialog(
            onDismissRequest = viewModel::hideTransitionDialog,
            title = { Text("Changer le statut") },
            text = {
                Column {
                    Text("Transitions autorisées depuis ${state.batch?.status?.displayName} :")
                    Spacer(Modifier.height(8.dp))
                    allowedStatuses.forEach { targetStatus ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = transitionTarget == targetStatus,
                                onClick = { transitionTarget = targetStatus }
                            )
                            Text(targetStatus.displayName)
                        }
                    }
                    if (transitionTarget == BatchStatus.DETRUIT) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = transitionReason,
                            onValueChange = { transitionReason = it },
                            label = { Text("Motif de destruction (obligatoire)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transitionTarget?.let { target ->
                            viewModel.transitionStatus(
                                batchId = batchId,
                                newStatus = target,
                                operatorId = currentUserId,
                                reason = transitionReason
                            )
                        }
                    },
                    enabled = transitionTarget != null &&
                        (transitionTarget != BatchStatus.DETRUIT || transitionReason.isNotBlank())
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideTransitionDialog) { Text("Annuler") }
            }
        )
    }

    // Dialog : Note de correction
    if (state.showCorrectionNoteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideCorrectionNoteDialog,
            title = { Text("Ajouter une note de correction") },
            text = {
                Column {
                    Text(
                        "⚠️ Le lot est clôturé. Seule une note de correction avec justification est autorisée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = correctionType,
                        onValueChange = { correctionType = it },
                        label = { Text("Type de correction") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = correctionJustification,
                        onValueChange = { correctionJustification = it },
                        label = { Text("Justification (obligatoire)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCorrectionNote(
                            batchId = batchId,
                            operatorId = currentUserId,
                            justification = correctionJustification,
                            correctionType = correctionType
                        )
                    },
                    enabled = correctionJustification.isNotBlank()
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideCorrectionNoteDialog) { Text("Annuler") }
            }
        )
    }
}

/**
 * Retourne les transitions autorisées depuis le statut actuel du lot.
 */
@Composable
private fun TransitionBatchStatusUseCase_companion_valid(batch: com.cannatrace.domain.model.Batch?): List<BatchStatus> {
    if (batch == null || batch.isClosed) return emptyList()
    return com.cannatrace.domain.usecase.TransitionBatchStatusUseCase.VALID_TRANSITIONS[batch.status]?.toList() ?: emptyList()
}

@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun CorrectionNoteCard(note: CorrectionNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.correctionType.ifBlank { "Correction" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = note.createdAt.toDisplayDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.justification,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Opérateur : ${note.operatorId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
