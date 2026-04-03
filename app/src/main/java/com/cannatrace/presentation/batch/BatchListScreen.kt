package com.cannatrace.presentation.batch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lots") },
                actions = {
                    IconButton(onClick = viewModel::loadAllBatches) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Créer un lot")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::search,
                label = { Text("Rechercher un lot...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Filtres par statut
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedStatus == null,
                        onClick = { viewModel.filterByStatus(null) },
                        label = { Text("Tous") }
                    )
                }
                items(BatchStatus.values().toList()) { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.filterByStatus(status) },
                        label = { Text(status.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredBatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Aucun lot trouvé",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredBatches, key = { it.id }) { batch ->
                        BatchCard(
                            batch = batch,
                            onClick = { onNavigateToDetail(batch.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCard(batch: Batch, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (batch.isClosed)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = batch.batchNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (batch.isClosed) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge {
                            Text("Clôturé", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    text = batch.strainName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Poids : ${batch.weight}g | THC: ${batch.thcContent}% | CBD: ${batch.cbdContent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                BatchStatusChip(status = batch.status)
            }
        }
    }
}

@Composable
fun BatchStatusChip(status: BatchStatus) {
    val color = when (status) {
        BatchStatus.GRAINE, BatchStatus.GERMINATION -> MaterialTheme.colorScheme.tertiary
        BatchStatus.CROISSANCE -> MaterialTheme.colorScheme.secondary
        BatchStatus.RECOLTE, BatchStatus.SECHAGE -> MaterialTheme.colorScheme.primary
        BatchStatus.TRANSFORMATION, BatchStatus.CONDITIONNEMENT -> MaterialTheme.colorScheme.primary
        BatchStatus.CONTROLE_QUALITE -> MaterialTheme.colorScheme.secondary
        BatchStatus.STOCKAGE -> MaterialTheme.colorScheme.primary
        BatchStatus.DISPENSATION -> MaterialTheme.colorScheme.tertiary
        BatchStatus.DETRUIT -> MaterialTheme.colorScheme.error
        BatchStatus.CLOTURE -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
