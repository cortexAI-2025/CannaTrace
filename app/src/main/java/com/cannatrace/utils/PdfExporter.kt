package com.cannatrace.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.model.Batch
import com.cannatrace.utils.DateUtils.toDisplayDate
import com.cannatrace.utils.DateUtils.toDisplayDateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Générateur de rapports PDF pour l'ANSM.
 *
 * Utilise android.graphics.pdf.PdfDocument (natif Android, sans dépendance externe)
 * pour créer des rapports conformes aux exigences réglementaires françaises.
 *
 * Format de page : A4 (595 x 842 points à 72 dpi)
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f

    private val titlePaint = Paint().apply {
        textSize = 16f
        color = Color.BLACK
        isFakeBoldText = true
    }

    private val subtitlePaint = Paint().apply {
        textSize = 12f
        color = Color.DKGRAY
        isFakeBoldText = true
    }

    private val bodyPaint = Paint().apply {
        textSize = 10f
        color = Color.BLACK
    }

    private val smallPaint = Paint().apply {
        textSize = 8f
        color = Color.GRAY
    }

    private val headerLinePaint = Paint().apply {
        color = Color.parseColor("#2E7D32") // Vert ANSM
        strokeWidth = 2f
    }

    /**
     * Génère un rapport d'inventaire PDF pour l'ANSM.
     *
     * @param context       Contexte Android
     * @param batches       Liste des lots actifs
     * @param reportDate    Timestamp de la date du rapport
     * @return Fichier PDF généré dans le répertoire cache
     */
    @Throws(IOException::class)
    fun generateInventoryReport(
        context: Context,
        batches: List<Batch>,
        reportDate: Long = System.currentTimeMillis()
    ): File {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        // En-tête du rapport
        y = drawHeader(canvas, y, reportDate, "RAPPORT D'INVENTAIRE DES LOTS")

        // Résumé
        y += LINE_HEIGHT
        val activeBatches = batches.filter { !it.isClosed }
        canvas.drawText("Nombre total de lots : ${batches.size}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
        canvas.drawText("Lots actifs : ${activeBatches.size}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
        canvas.drawText("Lots clôturés : ${batches.count { it.isClosed }}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT * 1.5f

        // Tableau des lots
        y = drawTableHeader(
            canvas, y,
            listOf("N° Lot", "Souche", "Statut", "Poids (g)", "THC%", "CBD%")
        )

        batches.forEach { batch ->
            if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(
                    canvas, y,
                    listOf("N° Lot", "Souche", "Statut", "Poids (g)", "THC%", "CBD%")
                )
            }

            y = drawTableRow(
                canvas, y,
                listOf(
                    batch.batchNumber,
                    batch.strainName.take(20),
                    batch.status.displayName,
                    "%.1f".format(batch.weight),
                    "%.1f".format(batch.thcContent),
                    "%.1f".format(batch.cbdContent)
                )
            )
        }

        // Pied de page
        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Écriture du fichier
        val fileName = "rapport_inventaire_${reportDate}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return file
    }

    /**
     * Génère un rapport de piste d'audit PDF pour un lot spécifique.
     */
    @Throws(IOException::class)
    fun generateAuditTrailReport(
        context: Context,
        batch: Batch,
        auditLogs: List<AuditLog>,
        reportDate: Long = System.currentTimeMillis()
    ): File {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        y = drawHeader(canvas, y, reportDate, "PISTE D'AUDIT - LOT ${batch.batchNumber}")

        // Informations du lot
        y += LINE_HEIGHT
        canvas.drawText("Lot : ${batch.batchNumber}", MARGIN, y, subtitlePaint)
        y += LINE_HEIGHT
        canvas.drawText("Souche : ${batch.strainName}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
        canvas.drawText("Statut actuel : ${batch.status.displayName}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
        canvas.drawText("Créé le : ${batch.createdAt.toDisplayDateTime()}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT
        canvas.drawText("Clôturé : ${if (batch.isClosed) "OUI" else "NON"}", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT * 1.5f

        // Entête colonne audit
        y = drawTableHeader(
            canvas, y,
            listOf("Date/Heure", "Action", "Utilisateur", "Hash (8 car.)")
        )

        auditLogs.forEach { log ->
            if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(
                    canvas, y,
                    listOf("Date/Heure", "Action", "Utilisateur", "Hash (8 car.)")
                )
            }

            y = drawTableRow(
                canvas, y,
                listOf(
                    log.timestamp.toDisplayDateTime(),
                    log.action.replace("_", " ").take(25),
                    log.userId.take(12),
                    log.hash.take(8) + "..."
                )
            )
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        val fileName = "audit_${batch.batchNumber}_${reportDate}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return file
    }

    private fun drawHeader(canvas: Canvas, startY: Float, date: Long, title: String): Float {
        var y = startY
        // Bande verte en haut
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, headerLinePaint)
        y += 20f

        // Logo/titre de l'app
        canvas.drawText("CannaTrace", MARGIN, y, subtitlePaint)
        canvas.drawText(
            "Généré le : ${date.toDisplayDateTime()}",
            PAGE_WIDTH - 200f, y, smallPaint
        )
        y += LINE_HEIGHT * 1.5f

        // Titre principal
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += LINE_HEIGHT

        // Ligne de séparation
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, headerLinePaint)
        y += LINE_HEIGHT

        // Mention légale
        canvas.drawText(
            "Document confidentiel - Réglementé ANSM - Usage médical uniquement",
            MARGIN, y, smallPaint
        )
        y += LINE_HEIGHT * 1.5f

        return y
    }

    private fun drawTableHeader(canvas: Canvas, y: Float, columns: List<String>): Float {
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / columns.size
        val bgPaint = Paint().apply { color = Color.parseColor("#E8F5E9") }
        canvas.drawRect(MARGIN, y - LINE_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y + 4f, bgPaint)

        columns.forEachIndexed { index, col ->
            canvas.drawText(col, MARGIN + index * colWidth + 4f, y, subtitlePaint)
        }

        val linePaint = Paint().apply { color = Color.LTGRAY }
        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, linePaint)

        return y + LINE_HEIGHT
    }

    private fun drawTableRow(canvas: Canvas, y: Float, values: List<String>): Float {
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / values.size
        values.forEachIndexed { index, value ->
            canvas.drawText(value, MARGIN + index * colWidth + 4f, y, bodyPaint)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }
        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, linePaint)

        return y + LINE_HEIGHT
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerY = PAGE_HEIGHT - MARGIN / 2
        canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, headerLinePaint)
        canvas.drawText(
            "CannaTrace v1.0 - Traçabilité Cannabis Médical - Conforme ANSM - Page $pageNumber",
            MARGIN, footerY, smallPaint
        )
    }
}
