package com.cannatrace.utils

import java.security.MessageDigest

/**
 * Utilitaires pour la blockchain légère basée sur SHA-256.
 *
 * Chaque action critique génère un hash chaîné :
 * hash = SHA256(previousHash | timestamp | action | entityId | data)
 *
 * Cela garantit l'immutabilité de la piste d'audit (toute modification
 * d'un enregistrement invalide tous les hashes suivants).
 */
object HashUtils {

    private const val GENESIS_HASH = "GENESIS"
    private const val SEPARATOR = "|"

    /**
     * Calcule un hash SHA-256 pour un enregistrement d'audit.
     *
     * @param previousHash  Hash du log précédent (ou "GENESIS" pour le premier log)
     * @param timestamp     Horodatage de l'action (epoch ms)
     * @param action        Code de l'action (ex: "CREATE_BATCH", "TRANSITION_BATCH_STATUS")
     * @param entityId      Identifiant de l'entité concernée
     * @param data          Données supplémentaires de l'action
     * @return Hash hexadécimal SHA-256 de 64 caractères
     */
    fun computeHash(
        previousHash: String,
        timestamp: Long,
        action: String,
        entityId: String,
        data: String
    ): String {
        val input = listOf(previousHash, timestamp.toString(), action, entityId, data)
            .joinToString(SEPARATOR)
        return sha256(input)
    }

    /**
     * Vérifie qu'un hash stocké correspond aux données fournies.
     */
    fun verifyHash(
        storedHash: String,
        previousHash: String,
        timestamp: Long,
        action: String,
        entityId: String,
        data: String
    ): Boolean {
        val recomputed = computeHash(previousHash, timestamp, action, entityId, data)
        return recomputed == storedHash
    }

    /**
     * Hash SHA-256 d'une chaîne de caractères, retourné en hexadécimal.
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hash initial pour le premier enregistrement de la chaîne.
     */
    fun genesisHash(): String = GENESIS_HASH

    /**
     * Vérifie la cohérence d'une séquence de triplets (previousHash, hash, data).
     * Retourne l'indice du premier enregistrement invalide, ou -1 si tout est correct.
     */
    fun verifyChain(
        entries: List<Triple<String, Long, String>>, // (action, timestamp, entityId+data)
        hashes: List<String>
    ): Int {
        if (entries.size != hashes.size) return 0
        var previousHash = GENESIS_HASH
        for (i in entries.indices) {
            val (action, timestamp, entityData) = entries[i]
            val parts = entityData.split(SEPARATOR, limit = 2)
            val entityId = parts.getOrElse(0) { "" }
            val data = parts.getOrElse(1) { "" }
            val expected = computeHash(previousHash, timestamp, action, entityId, data)
            if (expected != hashes[i]) return i
            previousHash = hashes[i]
        }
        return -1
    }
}
