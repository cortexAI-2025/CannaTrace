package com.cannatrace.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Générateur de QR codes pour les étiquettes de lots et de plantes.
 * Utilise la bibliothèque ZXing (com.google.zxing:core).
 */
object QrCodeGenerator {

    /**
     * Génère un QR code Bitmap pour un identifiant de lot ou de plante.
     *
     * @param content   Contenu à encoder (ex: "LOT:LOT-2024-001" ou "PLANT:PLT-00123")
     * @param size      Taille en pixels (largeur = hauteur)
     * @return Bitmap du QR code, ou null en cas d'erreur
     */
    fun generate(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            null
        }
    }

    /**
     * Génère le contenu d'un QR code pour un lot avec toutes les informations réglementaires.
     * Format : JSON compact pour la lisibilité maximale par les systèmes tiers.
     */
    fun generateBatchQrContent(
        batchId: String,
        batchNumber: String,
        strainName: String,
        thcContent: Double,
        cbdContent: Double
    ): String {
        return """{"id":"$batchId","lot":"$batchNumber","souche":"$strainName","thc":$thcContent,"cbd":$cbdContent}"""
    }

    /**
     * Génère le contenu d'un QR code pour une plante/graine individuelle.
     */
    fun generatePlantQrContent(
        plantId: String,
        qrCode: String,
        batchId: String,
        strain: String
    ): String {
        return "CANNATRACE:PLANT:$qrCode:$plantId:$batchId:$strain"
    }

    /**
     * Parse le contenu d'un QR code scanné et retourne le type et l'identifiant.
     * Retourne null si le format n'est pas reconnu.
     */
    fun parseQrContent(content: String): QrParseResult? {
        return when {
            content.startsWith("CANNATRACE:PLANT:") -> {
                val parts = content.split(":")
                if (parts.size >= 4) {
                    QrParseResult(type = QrType.PLANT, id = parts[3], raw = content)
                } else null
            }
            content.startsWith("{") && content.contains("\"lot\"") -> {
                // Format JSON lot
                val idMatch = Regex("\"id\":\"([^\"]+)\"").find(content)
                val id = idMatch?.groupValues?.getOrNull(1)
                if (id != null) QrParseResult(type = QrType.BATCH, id = id, raw = content)
                else null
            }
            else -> QrParseResult(type = QrType.UNKNOWN, id = content, raw = content)
        }
    }

    data class QrParseResult(
        val type: QrType,
        val id: String,
        val raw: String
    )

    enum class QrType {
        BATCH, PLANT, UNKNOWN
    }
}
