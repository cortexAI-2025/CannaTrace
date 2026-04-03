package com.cannatrace.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Utilitaires de chiffrement pour la protection des données sensibles.
 * Utilise Android Keystore pour la gestion sécurisée des clés.
 */
object CryptoUtils {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "cannatrace_master_key"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    /**
     * Obtient ou crée la clé secrète dans l'Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    /**
     * Chiffre un texte sensible (ex: données patient, token d'authentification).
     * @return Paire (IV en base64, texte chiffré en base64)
     */
    fun encrypt(plaintext: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = android.util.Base64.encodeToString(cipher.iv, android.util.Base64.DEFAULT)
        val encrypted = android.util.Base64.encodeToString(
            cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)),
            android.util.Base64.DEFAULT
        )
        return Pair(iv, encrypted)
    }

    /**
     * Déchiffre un texte précédemment chiffré avec encrypt().
     */
    fun decrypt(ivBase64: String, encryptedBase64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(
            android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
        )
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Génère un identifiant anonymisé pour un patient (conforme RGPD).
     * L'identifiant est un hash non réversible qui permettra de relier
     * les dispensations sans exposer l'identité réelle.
     */
    fun generateAnonymousPatientId(patientIdentifier: String, salt: String): String {
        val input = "$salt|$patientIdentifier"
        return "PAT-" + HashUtils.sha256(input).take(16).uppercase()
    }

    /**
     * Génère une passphrase aléatoire sécurisée pour SQLCipher.
     */
    fun generateSecurePassphrase(length: Int = 32): ByteArray {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }
}
