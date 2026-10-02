package com.budgetapp.app.security

import android.content.Context
import android.database.DatabaseUtils
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

object SqlCipherDatabaseSupport {
    fun createFactory(
        context: Context,
        databaseName: String,
        passphrase: String,
    ): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        migratePlaintextDatabaseIfNeeded(context.getDatabasePath(databaseName), passphrase)
        return SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
    }

    private fun migratePlaintextDatabaseIfNeeded(
        databaseFile: File,
        passphrase: String,
    ) {
        if (!databaseFile.exists() || !databaseFile.isPlaintextSqlite()) return

        databaseFile.parentFile?.mkdirs()
        val encryptedFile = File(databaseFile.parentFile, "${databaseFile.name}.encrypted")
        val plaintextBackup = File(databaseFile.parentFile, "${databaseFile.name}.plaintext")
        encryptedFile.delete()
        plaintextBackup.delete()

        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, "", null)
        try {
            database.rawExecSQL("PRAGMA wal_checkpoint(FULL)")
            database.rawExecSQL(
                "ATTACH DATABASE ${databaseString(encryptedFile.absolutePath)} AS encrypted KEY ${databaseString(passphrase)}",
            )
            database.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            database.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            database.close()
        }

        check(databaseFile.renameTo(plaintextBackup)) {
            "Could not preserve plaintext database before encryption."
        }
        if (!encryptedFile.renameTo(databaseFile)) {
            plaintextBackup.renameTo(databaseFile)
            error("Could not activate encrypted database.")
        }
        plaintextBackup.delete()
        databaseFile.walFile().delete()
        databaseFile.shmFile().delete()
    }

    private fun File.isPlaintextSqlite(): Boolean {
        val header = ByteArray(SqliteHeader.size)
        inputStream().use { input ->
            if (input.read(header) != header.size) return false
        }
        return header.contentEquals(SqliteHeader)
    }

    private fun databaseString(value: String): String =
        DatabaseUtils.sqlEscapeString(value)

    private fun File.walFile(): File =
        File(parentFile, "$name-wal")

    private fun File.shmFile(): File =
        File(parentFile, "$name-shm")

    private val SqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
}
