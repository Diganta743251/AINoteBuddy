package com.ainotebuddy.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles encryption and decryption of note content using AES-GCM
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val transformation = "AES/GCM/NoPadding"
    private val keySize = 256
    private val ivSize = 12 // 96 bits for GCM
    private val tagLength = 128 // 128 bits for GCM

    /**
     * Encrypts the given plaintext using the key with the specified alias
     * @param alias The alias of the key to use for encryption
     * @param plaintext The text to encrypt
     * @return Base64 encoded string containing the IV and ciphertext
     */
    fun encrypt(alias: String, plaintext: String): String {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        
        // Combine IV and ciphertext into a single byte array
        val encryptedData = ByteArray(iv.size + ciphertext.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(ciphertext, 0, this, iv.size, ciphertext.size)
        }
        
        return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
    }

    /**
     * Decrypts the given ciphertext using the key with the specified alias
     * @param alias The alias of the key to use for decryption
     * @param encryptedData Base64 encoded string containing the IV and ciphertext
     * @return The decrypted plaintext
     */
    fun decrypt(alias: String, encryptedData: String): String {
        val key = getKey(alias) ?: throw SecurityException("Key not found: $alias")
        val encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
        
        // Extract IV and ciphertext
        val iv = encryptedBytes.copyOfRange(0, ivSize)
        val ciphertext = encryptedBytes.copyOfRange(ivSize, encryptedBytes.size)
        
        val cipher = Cipher.getInstance(transformation).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagLength, iv))
        }
        
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    /**
     * Deletes the key with the specified alias
     */
    fun deleteKey(alias: String) {
        keyStore.deleteEntry(alias)
    }

    /**
     * Checks if a key with the specified alias exists
     */
    fun keyExists(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        return getKey(alias) ?: createKey(alias)
    }

    private fun getKey(alias: String): SecretKey? {
        return try {
            (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun createKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(keySize)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
