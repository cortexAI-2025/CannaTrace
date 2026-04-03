package com.cannatrace

import com.cannatrace.domain.usecase.GenerateAuditHashUseCase
import com.cannatrace.utils.HashUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour le système de logs d'audit immuables (blockchain légère).
 *
 * Vérifie :
 * - La génération correcte des hashes
 * - La chaîne de hachage (chaque hash dépend du précédent)
 * - La détection de falsification
 */
class AuditLogTest {

    private lateinit var generateAuditHashUseCase: GenerateAuditHashUseCase

    @Before
    fun setup() {
        generateAuditHashUseCase = GenerateAuditHashUseCase()
    }

    @Test
    fun `premier log utilise GENESIS comme hash précédent`() {
        val hash = generateAuditHashUseCase.computeHash(
            previousHash = "GENESIS",
            timestamp = 1704067200000L,
            action = "CREATE_BATCH",
            entityId = "batch-001",
            data = "batchNumber=LOT-2024-001"
        )
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `deuxième log utilise le hash du premier comme previousHash`() {
        val timestamp1 = 1704067200000L
        val hash1 = generateAuditHashUseCase.computeHash(
            previousHash = "GENESIS",
            timestamp = timestamp1,
            action = "CREATE_BATCH",
            entityId = "batch-001",
            data = "batchNumber=LOT-2024-001"
        )

        val timestamp2 = 1704067260000L
        val hash2 = generateAuditHashUseCase.computeHash(
            previousHash = hash1,
            timestamp = timestamp2,
            action = "TRANSITION_BATCH_STATUS",
            entityId = "batch-001",
            data = "from=GRAINE;to=GERMINATION"
        )

        assertNotNull(hash2)
        assertNotEquals("Les deux hashes doivent être différents", hash1, hash2)
    }

    @Test
    fun `verifyHash retourne true pour un hash valide`() {
        val previousHash = "GENESIS"
        val timestamp = 1704067200000L
        val action = "CREATE_BATCH"
        val entityId = "batch-001"
        val data = "batchNumber=LOT-2024-001"

        val hash = generateAuditHashUseCase.computeHash(previousHash, timestamp, action, entityId, data)

        assertTrue(
            generateAuditHashUseCase.verifyHash(hash, previousHash, timestamp, action, entityId, data)
        )
    }

    @Test
    fun `verifyHash retourne false si les données ont été modifiées`() {
        val previousHash = "GENESIS"
        val timestamp = 1704067200000L
        val action = "CREATE_BATCH"
        val entityId = "batch-001"
        val data = "batchNumber=LOT-2024-001"

        val hash = generateAuditHashUseCase.computeHash(previousHash, timestamp, action, entityId, data)

        // Tentative de falsification de la donnée
        assertFalse(
            "Une donnée falsifiée doit invalider le hash",
            generateAuditHashUseCase.verifyHash(
                hash, previousHash, timestamp, action, entityId,
                "batchNumber=LOT-2024-001;weight=99999"  // Donnée falsifiée
            )
        )
    }

    @Test
    fun `modifie un timestamp invalide le hash`() {
        val hash = generateAuditHashUseCase.computeHash(
            "GENESIS", 1000L, "ACTION", "id", "data"
        )
        assertFalse(
            "Un timestamp modifié doit invalider le hash",
            generateAuditHashUseCase.verifyHash(hash, "GENESIS", 9999L, "ACTION", "id", "data")
        )
    }

    @Test
    fun `une chaine de 5 logs est cohérente`() {
        val actions = listOf(
            Triple("CREATE_BATCH", "batch-001", "batchNumber=LOT-001"),
            Triple("TRANSITION_BATCH_STATUS", "batch-001", "from=GRAINE;to=GERMINATION"),
            Triple("TRANSITION_BATCH_STATUS", "batch-001", "from=GERMINATION;to=CROISSANCE"),
            Triple("ADD_CULTURE_ENTRY", "batch-001", "type=TEMPERATURE;value=22.5"),
            Triple("TRANSITION_BATCH_STATUS", "batch-001", "from=CROISSANCE;to=RECOLTE")
        )

        val hashes = mutableListOf<String>()
        var previousHash = "GENESIS"

        for ((index, action) in actions.withIndex()) {
            val timestamp = 1704067200000L + index * 60000L
            val hash = generateAuditHashUseCase.computeHash(
                previousHash, timestamp, action.first, action.second, action.third
            )
            hashes.add(hash)
            previousHash = hash
        }

        // Vérification de chaque entrée de la chaîne
        previousHash = "GENESIS"
        for (i in actions.indices) {
            val timestamp = 1704067200000L + i * 60000L
            val (actionStr, entityId, data) = actions[i]
            assertTrue(
                "Log $i de la chaîne doit être valide",
                generateAuditHashUseCase.verifyHash(
                    hashes[i], previousHash, timestamp, actionStr, entityId, data
                )
            )
            previousHash = hashes[i]
        }
    }

    @Test
    fun `falsification d'un log intermédiaire est détectée`() {
        // Création d'une chaîne de 3 logs
        val hash1 = HashUtils.computeHash("GENESIS", 1000L, "CREATE", "id", "data1")
        val hash2 = HashUtils.computeHash(hash1, 2000L, "UPDATE", "id", "data2")
        val hash3 = HashUtils.computeHash(hash2, 3000L, "CLOSE", "id", "data3")

        // Falsification du log 2 (données modifiées)
        val hash2_falsifie = HashUtils.computeHash(hash1, 2000L, "UPDATE", "id", "FALSIFIE")

        // hash3 avec le vrai hash2 est valide
        assertTrue(HashUtils.verifyHash(hash3, hash2, 3000L, "CLOSE", "id", "data3"))

        // hash3 avec hash2 falsifié est invalide (le hash précédent ne correspond pas)
        assertFalse(
            "La falsification d'un log intermédiaire doit être détectée",
            HashUtils.verifyHash(hash3, hash2_falsifie, 3000L, "CLOSE", "id", "data3")
        )
    }
}
