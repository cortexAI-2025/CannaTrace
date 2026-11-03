package com.cannatrace

import com.cannatrace.domain.model.*
import com.cannatrace.domain.repository.*
import com.cannatrace.domain.usecase.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.util.UUID

class QrLookupTest {
    @Test fun knownOpaqueQrResolvesLocalBatch() = runTest {
        val batches = mock<BatchRepository>()
        val plants = mock<PlantRepository>()
        val id = UUID.randomUUID().toString()
        val batch = Batch(id, "DEMO-$id", "Fictif", BatchStatus.STOCKAGE, 100.0, 0.0, 0.0, 1L, ownerId = "demo-local", locationId = "Zone A")
        whenever(batches.getBatchById(id)).thenReturn(batch)
        val result = ScanQrCodeUseCase(plants, batches)(TraceRules.qr(id))
        assertEquals(batch, (result as QrScanResult.BatchFound).batch)
        verifyNoInteractions(plants)
    }
    @Test fun unknownOpaqueQrStaysUnknown() = runTest {
        val result = ScanQrCodeUseCase(mock(), mock())(TraceRules.qr(UUID.randomUUID().toString()))
        assertTrue(result is QrScanResult.UnknownCode)
    }
    @Test fun malformedQrIsRejectedBeforeAnyLookup() = runTest {
        val batches = mock<BatchRepository>(); val plants = mock<PlantRepository>()
        val result = ScanQrCodeUseCase(plants, batches)("cannatrace:v1:lot:not-an-id")
        assertTrue(result is QrScanResult.Error)
        verifyNoInteractions(plants, batches)
    }
}
