package com.cannatrace

import com.cannatrace.utils.HashUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la logique de hachage SHA-256 (blockchain légère).
 */
class HashUtilsTest {

    @Test
    fun `computeHash returns non-empty 64-char hex string`() {
        val hash = HashUtils.computeHash(
            previousHash = "GENESIS",
            timestamp = 1704067200000L,
            action = "CREATE_BATCH",
            entityId = "batch-001",
            data = "batchNumber=LOT-2024-001;strain=Bedrocan"
        )
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `computeHash is deterministic`() {
        val params = listOf(
            "GENESIS", 1704067200000L, "CREATE_BATCH", "batch-001",
            "batchNumber=LOT-2024-001"
        )
        val hash1 = HashUtils.computeHash(
            params[0] as String, params[1] as Long,
            params[2] as String, params[3] as String, params[4] as String
        )
        val hash2 = HashUtils.computeHash(
            params[0] as String, params[1] as Long,
            params[2] as String, params[3] as String, params[4] as String
        )
        assertEquals("Le hash doit être déterministe", hash1, hash2)
    }

    @Test
    fun `different inputs produce different hashes`() {
        val hash1 = HashUtils.computeHash("GENESIS", 1000L, "CREATE", "id1", "data1")
        val hash2 = HashUtils.computeHash("GENESIS", 1000L, "CREATE", "id1", "data2")
        assertNotEquals("Des données différentes doivent produire des hashes différents", hash1, hash2)
    }

    @Test
    fun `verifyHash returns true for matching data`() {
        val previousHash = "GENESIS"
        val timestamp = 1704067200000L
        val action = "TRANSITION_BATCH_STATUS"
        val entityId = "batch-001"
        val data = "from=GRAINE;to=GERMINATION"

        val hash = HashUtils.computeHash(previousHash, timestamp, action, entityId, data)

        assertTrue(
            "verifyHash doit retourner true pour les données correctes",
            HashUtils.verifyHash(hash, previousHash, timestamp, action, entityId, data)
        )
    }

    @Test
    fun `verifyHash returns false for tampered data`() {
        val previousHash = "GENESIS"
        val timestamp = 1704067200000L
        val action = "CREATE_BATCH"
        val entityId = "batch-001"
        val data = "batchNumber=LOT-2024-001"

        val hash = HashUtils.computeHash(previousHash, timestamp, action, entityId, data)

        assertFalse(
            "verifyHash doit retourner false si les données ont été falsifiées",
            HashUtils.verifyHash(hash, previousHash, timestamp, action, entityId, "FALSIFIE")
        )
    }

    @Test
    fun `chain changes when previous hash changes`() {
        val hash1 = HashUtils.computeHash("HASH_PREVIOUS_1", 1000L, "ACTION", "id", "data")
        val hash2 = HashUtils.computeHash("HASH_PREVIOUS_2", 1000L, "ACTION", "id", "data")
        assertNotEquals(
            "Le changement du hash précédent doit produire un hash différent",
            hash1, hash2
        )
    }

    @Test
    fun `sha256 produces known result`() {
        // Test avec une valeur connue : SHA256("") = e3b0c44298fc1c149afb...
        val emptyHash = HashUtils.sha256("")
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            emptyHash
        )
    }

    @Test
    fun `genesisHash returns GENESIS`() {
        assertEquals("GENESIS", HashUtils.genesisHash())
    }
}
