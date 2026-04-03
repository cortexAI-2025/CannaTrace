package com.cannatrace.utils

import android.content.Context
import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.StockEntry
import com.cannatrace.utils.DateUtils.toDisplayDateTime
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Exportateur CSV pour les rapports d'inventaire et d'audit ANSM.
 * Les fichiers CSV sont générés dans le répertoire de cache de l'application.
 */
object CsvExporter {

    private const val CSV_SEPARATOR = ";"
    private const val LINE_SEPARATOR = "\n"
    private const val ENCODING = "UTF-8"

    /**
     * Exporte la liste des lots actifs au format CSV pour l'ANSM.
     * Format attendu : NuméroLot;Souche;Statut;Poids(g);THC%;CBD%;DateCréation;Propriétaire;Localisation
     */
    @Throws(IOException::class)
    fun exportBatches(context: Context, batches: List<Batch>): File {
        val fileName = "inventaire_lots_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file, Charsets.UTF_8).use { writer ->
            // En-tête
            writer.write(
                listOf(
                    "Numéro de Lot", "Souche", "Statut", "Poids (g)",
                    "THC (%)", "CBD (%)", "Date Création", "ID Propriétaire",
                    "ID Localisation", "Clôturé", "Hash Chaîne"
                ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
            )

            // Données
            batches.forEach { batch ->
                writer.write(
                    listOf(
                        batch.batchNumber.escapeCsv(),
                        batch.strainName.escapeCsv(),
                        batch.status.displayName.escapeCsv(),
                        batch.weight.toString(),
                        batch.thcContent.toString(),
                        batch.cbdContent.toString(),
                        batch.createdAt.toDisplayDateTime().escapeCsv(),
                        batch.ownerId.escapeCsv(),
                        batch.locationId.escapeCsv(),
                        if (batch.isClosed) "OUI" else "NON",
                        batch.hashChain.escapeCsv()
                    ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
                )
            }
        }

        return file
    }

    /**
     * Exporte les mouvements de stock au format CSV.
     * Format : ID;IDLot;TypeMouvement;Quantité;Unité;Motif;IDOpérateur;DateHeure
     */
    @Throws(IOException::class)
    fun exportStockEntries(context: Context, entries: List<StockEntry>): File {
        val fileName = "mouvements_stock_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.write(
                listOf(
                    "ID", "Numéro de Lot", "Type de Mouvement",
                    "Quantité", "Unité", "Motif", "ID Opérateur", "Date/Heure"
                ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
            )

            entries.forEach { entry ->
                writer.write(
                    listOf(
                        entry.id.escapeCsv(),
                        entry.batchId.escapeCsv(),
                        entry.movementType.displayName.escapeCsv(),
                        entry.quantity.toString(),
                        entry.unit.escapeCsv(),
                        entry.reason.escapeCsv(),
                        entry.operatorId.escapeCsv(),
                        entry.timestamp.toDisplayDateTime().escapeCsv()
                    ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
                )
            }
        }

        return file
    }

    /**
     * Exporte les logs d'audit au format CSV pour transmission à l'ANSM.
     * Format : ID;Action;TypeEntité;IDEntité;IDUtilisateur;DateHeure;Hash;HashPrécédent;Données
     */
    @Throws(IOException::class)
    fun exportAuditLogs(context: Context, logs: List<AuditLog>): File {
        val fileName = "audit_trail_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.write(
                listOf(
                    "ID", "Action", "Type Entité", "ID Entité",
                    "ID Utilisateur", "Date/Heure", "Hash SHA-256",
                    "Hash Précédent", "Données"
                ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
            )

            logs.forEach { log ->
                writer.write(
                    listOf(
                        log.id.escapeCsv(),
                        log.action.escapeCsv(),
                        log.entityType.escapeCsv(),
                        log.entityId.escapeCsv(),
                        log.userId.escapeCsv(),
                        log.timestamp.toDisplayDateTime().escapeCsv(),
                        log.hash.escapeCsv(),
                        log.previousHash.escapeCsv(),
                        log.data.escapeCsv()
                    ).joinToString(CSV_SEPARATOR) + LINE_SEPARATOR
                )
            }
        }

        return file
    }

    /**
     * Échappe une valeur pour le format CSV (entoure de guillemets si nécessaire).
     */
    private fun String.escapeCsv(): String {
        return if (contains(CSV_SEPARATOR) || contains("\"") || contains(LINE_SEPARATOR)) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
    }
}
