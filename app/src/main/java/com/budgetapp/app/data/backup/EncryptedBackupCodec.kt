package com.budgetapp.app.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedBackupCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encrypt(payload: BudgetBackupPayload): ByteArray {
        val plaintext = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encryptedFile = EncryptedBackupFile(
            encryptedAtMillis = System.currentTimeMillis(),
            iv = base64Encode(cipher.iv),
            ciphertext = base64Encode(cipher.doFinal(plaintext)),
        )
        return json.encodeToString(encryptedFile).toByteArray(Charsets.UTF_8)
    }

    fun decrypt(bytes: ByteArray): BudgetBackupPayload {
        val encryptedFile = runCatching {
            json.decodeFromString<EncryptedBackupFile>(bytes.toString(Charsets.UTF_8))
        }.getOrElse {
            throw IllegalArgumentException("This is not a Chap backup file.")
        }

        require(encryptedFile.format == BackupFormat) { "This is not a Chap backup file." }
        require(encryptedFile.version == EncryptedFileVersion) { "Unsupported backup file version." }
        require(encryptedFile.algorithm == BackupAlgorithm) { "Unsupported backup encryption." }

        val cipher = Cipher.getInstance(AesGcmTransformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GcmTagBits, base64Decode(encryptedFile.iv)),
        )
        val plaintext = runCatching {
            cipher.doFinal(base64Decode(encryptedFile.ciphertext))
        }.getOrElse {
            throw IllegalArgumentException("This backup cannot be decrypted on this device.")
        }
        return runCatching {
            json.decodeFromString<BudgetBackupPayload>(plaintext.toString(Charsets.UTF_8))
        }.getOrElse {
            throw IllegalArgumentException("This backup file is damaged.")
        }
    }

    fun deleteLocalKey() {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        if (keyStore.containsAlias(KeyAlias)) {
            keyStore.deleteEntry(KeyAlias)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        val existingKey = keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val spec = KeyGenParameterSpec.Builder(
            KeyAlias,
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

    private fun base64Encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun base64Decode(value: String): ByteArray =
        runCatching { Base64.getDecoder().decode(value) }
            .getOrElse { throw IllegalArgumentException("This backup file is damaged.") }

    private companion object {
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "chap_budget_backup_v1"
        const val AesGcmTransformation = "AES/GCM/NoPadding"
        const val AesKeyBits = 256
        const val GcmTagBits = 128
        const val BackupFormat = "chap-budget-backup"
        const val BackupAlgorithm = "AES-256-GCM"
        const val EncryptedFileVersion = 1
    }
}

@Serializable
private data class EncryptedBackupFile(
    val format: String = "chap-budget-backup",
    val version: Int = 1,
    val algorithm: String = "AES-256-GCM",
    @SerialName("encrypted_at_millis") val encryptedAtMillis: Long,
    val iv: String,
    val ciphertext: String,
)
