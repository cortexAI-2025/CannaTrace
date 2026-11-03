package com.cannatrace

import com.cannatrace.domain.model.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TraceRulesTest {
    @Test fun opaqueQrRoundTripAndImageDecode() {
        val id = UUID.randomUUID().toString()
        val raw = TraceRules.qr(id)
        assertEquals(id, TraceRules.parseQr(raw))
        val matrix = QRCodeWriter().encode(raw, BarcodeFormat.QR_CODE, 256, 256)
        val pixels = IntArray(256 * 256) { i -> if (matrix[i % 256, i / 256]) 0xff000000.toInt() else 0xffffffff.toInt() }
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(256, 256, pixels)))).text
        assertEquals(raw, decoded)
        assertEquals(id, TraceRules.parseQr(decoded))
    }
    @Test fun invalidQrRejected() {
        listOf("", "https://evil.invalid/lot", "cannatrace:v1:lot:secret", "cannatrace:v2:lot:${UUID.randomUUID()}").forEach { assertNull(TraceRules.parseQr(it)) }
    }
    @Test fun quantityValidation() {
        listOf("-1", "NaN", "Infinity", "0.0001", "1000000001", "abc").forEach { assertTrue(it, runCatching { TraceRules.quantity(it) }.isFailure) }
        assertEquals("12.125", TraceRules.quantity("12,125").toPlainString())
    }
    @Test fun stockCannotBecomeNegativeOrOverflow() {
        assertEquals(12.5, TraceRules.balance("10", "ENTREE", "2.5").toDouble(), 0.0)
        assertEquals(7.5, TraceRules.balance("10", "SORTIE", "2.5").toDouble(), 0.0)
        assertEquals(0.0, TraceRules.balance("10", "CORRECTION", "0").toDouble(), 0.0)
        assertTrue(runCatching { TraceRules.balance("10", "SORTIE", "11") }.isFailure)
        assertTrue(runCatching { TraceRules.balance("10", "ENTREE", "0") }.isFailure)
        assertTrue(runCatching { TraceRules.balance("1000000000", "ENTREE", "1") }.isFailure)
    }
    @Test fun rolesEnforced() {
        assertTrue(runCatching { TraceRules.authorize(DemoRole.LECTEUR, "CREATE") }.isFailure)
        assertTrue(runCatching { TraceRules.authorize(DemoRole.OPERATEUR, "ARCHIVE") }.isFailure)
        TraceRules.authorize(DemoRole.ADMIN, "ARCHIVE")
        TraceRules.authorize(DemoRole.OPERATEUR, "ENTREE")
    }
    @Test fun generatedIdentifiersUnique() {
        val ids = (1..10000).map { UUID.randomUUID().toString() }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertEquals(it, TraceRules.uuid(it)) }
    }
}
