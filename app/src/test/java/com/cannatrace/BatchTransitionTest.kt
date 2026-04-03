package com.cannatrace

import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.usecase.TransitionBatchStatusUseCase
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la machine d'états des lots (seed-to-sale pipeline).
 * Vérifie que les transitions valides sont autorisées et que les transitions
 * invalides sont refusées conformément aux exigences réglementaires ANSM.
 */
class BatchTransitionTest {

    private val validTransitions = TransitionBatchStatusUseCase.VALID_TRANSITIONS

    // ─── Transitions valides (pipeline complet) ──────────────────────────

    @Test
    fun `GRAINE peut transitionner vers GERMINATION`() {
        assertTrue(isValidTransition(BatchStatus.GRAINE, BatchStatus.GERMINATION))
    }

    @Test
    fun `GERMINATION peut transitionner vers CROISSANCE`() {
        assertTrue(isValidTransition(BatchStatus.GERMINATION, BatchStatus.CROISSANCE))
    }

    @Test
    fun `CROISSANCE peut transitionner vers RECOLTE`() {
        assertTrue(isValidTransition(BatchStatus.CROISSANCE, BatchStatus.RECOLTE))
    }

    @Test
    fun `RECOLTE peut transitionner vers SECHAGE`() {
        assertTrue(isValidTransition(BatchStatus.RECOLTE, BatchStatus.SECHAGE))
    }

    @Test
    fun `SECHAGE peut transitionner vers TRANSFORMATION`() {
        assertTrue(isValidTransition(BatchStatus.SECHAGE, BatchStatus.TRANSFORMATION))
    }

    @Test
    fun `TRANSFORMATION peut transitionner vers CONDITIONNEMENT`() {
        assertTrue(isValidTransition(BatchStatus.TRANSFORMATION, BatchStatus.CONDITIONNEMENT))
    }

    @Test
    fun `CONDITIONNEMENT peut transitionner vers CONTROLE_QUALITE`() {
        assertTrue(isValidTransition(BatchStatus.CONDITIONNEMENT, BatchStatus.CONTROLE_QUALITE))
    }

    @Test
    fun `CONTROLE_QUALITE peut transitionner vers STOCKAGE`() {
        assertTrue(isValidTransition(BatchStatus.CONTROLE_QUALITE, BatchStatus.STOCKAGE))
    }

    @Test
    fun `STOCKAGE peut transitionner vers DISPENSATION`() {
        assertTrue(isValidTransition(BatchStatus.STOCKAGE, BatchStatus.DISPENSATION))
    }

    @Test
    fun `DISPENSATION peut transitionner vers CLOTURE`() {
        assertTrue(isValidTransition(BatchStatus.DISPENSATION, BatchStatus.CLOTURE))
    }

    // ─── Transitions vers DETRUIT (depuis tous les statuts actifs) ───────

    @Test
    fun `tout statut actif peut transitionner vers DETRUIT`() {
        val destructibleStatuses = listOf(
            BatchStatus.GRAINE, BatchStatus.GERMINATION, BatchStatus.CROISSANCE,
            BatchStatus.RECOLTE, BatchStatus.SECHAGE, BatchStatus.TRANSFORMATION,
            BatchStatus.CONDITIONNEMENT, BatchStatus.CONTROLE_QUALITE,
            BatchStatus.STOCKAGE, BatchStatus.DISPENSATION
        )
        destructibleStatuses.forEach { status ->
            assertTrue(
                "Le statut $status doit pouvoir transitionner vers DETRUIT",
                isValidTransition(status, BatchStatus.DETRUIT)
            )
        }
    }

    // ─── Transitions invalides (retours en arrière interdits) ────────────

    @Test
    fun `GERMINATION ne peut pas revenir a GRAINE`() {
        assertFalse(isValidTransition(BatchStatus.GERMINATION, BatchStatus.GRAINE))
    }

    @Test
    fun `STOCKAGE ne peut pas aller vers GERMINATION`() {
        assertFalse(isValidTransition(BatchStatus.STOCKAGE, BatchStatus.GERMINATION))
    }

    @Test
    fun `RECOLTE ne peut pas aller vers CONDITIONNEMENT sans passer par les étapes intermédiaires`() {
        assertFalse(isValidTransition(BatchStatus.RECOLTE, BatchStatus.CONDITIONNEMENT))
    }

    @Test
    fun `CLOTURE n'a aucune transition vers un statut actif`() {
        val activeStatuses = BatchStatus.values().filter {
            it != BatchStatus.CLOTURE
        }
        activeStatuses.forEach { targetStatus ->
            assertFalse(
                "CLOTURE ne doit pas pouvoir transitionner vers $targetStatus",
                isValidTransition(BatchStatus.CLOTURE, targetStatus)
            )
        }
    }

    @Test
    fun `GRAINE ne peut pas aller directement a STOCKAGE`() {
        assertFalse(isValidTransition(BatchStatus.GRAINE, BatchStatus.STOCKAGE))
    }

    @Test
    fun `GRAINE ne peut pas aller a CLOTURE directement`() {
        assertFalse(isValidTransition(BatchStatus.GRAINE, BatchStatus.CLOTURE))
    }

    // ─── Pipeline complet validé ─────────────────────────────────────────

    @Test
    fun `pipeline complet seed-to-sale est valide`() {
        val pipeline = listOf(
            BatchStatus.GRAINE,
            BatchStatus.GERMINATION,
            BatchStatus.CROISSANCE,
            BatchStatus.RECOLTE,
            BatchStatus.SECHAGE,
            BatchStatus.TRANSFORMATION,
            BatchStatus.CONDITIONNEMENT,
            BatchStatus.CONTROLE_QUALITE,
            BatchStatus.STOCKAGE,
            BatchStatus.DISPENSATION,
            BatchStatus.CLOTURE
        )

        for (i in 0 until pipeline.size - 1) {
            val from = pipeline[i]
            val to = pipeline[i + 1]
            assertTrue(
                "Transition invalide dans le pipeline : $from → $to",
                isValidTransition(from, to)
            )
        }
    }

    // ─── Utilitaire ──────────────────────────────────────────────────────

    private fun isValidTransition(from: BatchStatus, to: BatchStatus): Boolean {
        return validTransitions[from]?.contains(to) == true
    }
}
