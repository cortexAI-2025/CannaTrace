package com.cannatrace.presentation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rapports & Exports") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Rapports ANSM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Génération de rapports conformes aux exigences de l'Agence Nationale de Sécurité du Médicament.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                ReportCard(
                    title = "Inventaire des lots",
                    description = "Export de tous les lots actifs avec poids, teneurs THC/CBD et statuts.",
                    icon = Icons.Default.Inventory,
                    isLoading = state.isGeneratingInventoryPdf,
                    onExportPdf = viewModel::generateInventoryPdf,
                    onExportCsv = viewModel::generateInventoryCsv
                )
            }

            item {
                ReportCard(
                    title = "Piste d'audit complète",
                    description = "Journal d'audit avec hashes SHA-256 chainés pour vérification d'intégrité.",
                    icon = Icons.Default.Security,
                    isLoading = state.isGeneratingAuditPdf,
                    onExportPdf = viewModel::generateAuditPdf,
                    onExportCsv = viewModel::generateAuditCsv
                )
            }

            item {
                ReportCard(
                    title = "Mouvements de stock",
                    description = "Historique complet des entrées/sorties avec motifs et opérateurs.",
                    icon = Icons.Default.SwapVert,
                    isLoading = state.isGeneratingStockReport,
                    onExportCsv = viewModel::generateStockCsv
                )
            }

            if (state.exportedFile != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Fichier généré avec succès",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    state.exportedFile,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Erreur : ${state.error}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vérification d'intégrité",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Vérifier la chaîne de hachage",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Vérifie que tous les logs d'audit sont cohérents et non modifiés.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::verifyHashChain,
                            enabled = !state.isVerifyingChain
                        ) {
                            if (state.isVerifyingChain) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Vérifier l'intégrité")
                            }
                        }
                        state.chainVerificationResult?.let { result ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (result) "✅ Chaîne de hachage valide — aucune falsification détectée."
                                       else "❌ ALERTE : Incohérence détectée dans la chaîne d'audit !",
                                color = if (result) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onExportPdf: (() -> Unit)? = null,
    onExportCsv: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onExportPdf != null) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
                if (onExportCsv != null) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CSV")
                    }
                }
            }
        }
    }
}
