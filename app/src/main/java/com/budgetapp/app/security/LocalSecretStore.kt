package com.budgetapp.app.security

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalSecretStore(
    private val preferences: SharedPreferences,
) {
    fun getOrCreateDatabasePassphrase(): String {
        preferences.getString(DatabasePassphraseKey, null)?.let { encoded ->
            return decrypt(encoded)
        }

        val secretBytes = ByteArray(DatabasePassphraseBytes).also(SecureRandom()::nextBytes)
        val passphrase = Base64.getEncoder().encodeToString(secretBytes)
        check(preferences.edit().putString(DatabasePassphraseKey, encrypt(passphrase)).commit()) {
            "Could not save the local database key."
        }
        return passphrase
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(base64(cipher.iv), base64(ciphertext)).joinToString(EnvelopeSeparator)
    }

    private fun decrypt(encoded: String): String {
        val parts = encoded.split(EnvelopeSeparator)
        require(parts.size == 2) { "Local database key is damaged." }
        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GcmTagBits, decodeBase64(parts[0])),
        )
        return cipher.doFinal(decodeBase64(parts[1])).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        val existingKey = keyStore.getEntry(DatabaseKeyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val spec = KeyGenParameterSpec.Builder(
            DatabaseKeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AesKeyBits)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun base64(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decodeBase64(value: String): ByteArray =
        Base64.getDecoder().decode(value)

    private companion object {
        const val AndroidKeyStore = "AndroidKeyStore"
        const val DatabaseKeyAlias = "chap_budget_database_secret_v1"
        const val DatabasePassphraseKey = "database_passphrase"
        const val DatabasePassphraseBytes = 32
        const val AesGcmTransformation = "AES/GCM/NoPadding"
        const val AesKeyBits = 256
        const val GcmTagBits = 128
        const val EnvelopeSeparator = ":"
    }
}
