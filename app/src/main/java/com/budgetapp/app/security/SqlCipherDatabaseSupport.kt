package com.budgetapp.app.security

import android.content.Context
import android.database.DatabaseUtils
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

object SqlCipherDatabaseSupport {
    fun createFactory(
        context: Context,
        databaseName: String,
        passphrase: String,
    ): SupportOpenHelperFactory {
        System.loadLibrary("sqlcipher")
        migratePlaintextDatabaseIfNeeded(context.getDatabasePath(databaseName), passphrase)
        return SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))
    }

    private fun migratePlaintextDatabaseIfNeeded(
        databaseFile: File,
        passphrase: String,
    ) {
        val encryptedFile = File(databaseFile.parentFile, "${databaseFile.name}.encrypted")
        val plaintextBackup = File(databaseFile.parentFile, "${databaseFile.name}.plaintext")
        // A process can die between the two renames. Recover the source before Room opens
        // a new empty database and strands the user's existing budget.
        if (!databaseFile.exists() && plaintextBackup.exists()) {
            check(plaintextBackup.renameTo(databaseFile)) { "Could not recover database during encryption." }
        }
        if (!databaseFile.exists() || !databaseFile.isPlaintextSqlite()) return

        databaseFile.parentFile?.mkdirs()
        encryptedFile.delete()
        plaintextBackup.delete()

        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, "", null, null)
        try {
            database.rawQuery("PRAGMA wal_checkpoint(FULL)", emptyArray<String>()).use { it.moveToFirst() }
            database.execSQL(
                "ATTACH DATABASE ${databaseString(encryptedFile.absolutePath)} AS encrypted KEY ${databaseString(passphrase)}",
            )
            database.rawQuery("SELECT sqlcipher_export('encrypted')", emptyArray<String>()).use { it.moveToFirst() }
            // sqlcipher_export copies tables, but not the version Room uses for migrations.
            database.execSQL("PRAGMA encrypted.user_version = ${database.version}")
            database.execSQL("DETACH DATABASE encrypted")
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
