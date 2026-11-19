package com.budgetapp.app.security

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.budgetapp.app.data.BudgetDatabase
import com.budgetapp.app.data.entity.EnvelopeEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionTest {
    @Test
    fun encryptingAnExistingRoomDatabasePreservesItsVersionAndRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-${System.nanoTime()}.db"
        val plaintext = Room.databaseBuilder(context, BudgetDatabase::class.java, name).build()
        val id = plaintext.budgetDao().insertEnvelope(EnvelopeEntity(
            name = "Migration test", ceilingCents = 12345, colorToken = "variable",
            sortOrder = 0,
            createdAtMillis = 1, updatedAtMillis = 1,
        ))
        plaintext.openHelper.writableDatabase.version = 4
        plaintext.close()
        // Simulate an interrupted conversion: the original was renamed but the replacement
        // was not activated. The next launch must recover this budget, then encrypt it.
        val file = context.getDatabasePath(name)
        val preserved = File(file.parentFile, "$name.plaintext")
        check(file.renameTo(preserved))
        val encrypted = Room.databaseBuilder(context, BudgetDatabase::class.java, name)
            .openHelperFactory(SqlCipherDatabaseSupport.createFactory(context, name, "test-passphrase"))
            .addMigrations(BudgetDatabase.Migration4To5)
            .build()
        try {
            assertEquals(5, encrypted.openHelper.readableDatabase.version)
            assertEquals(id, encrypted.budgetDao().getAllEnvelopes().single().id)
            assertEquals(12345, encrypted.budgetDao().getAllEnvelopes().single().ceilingCents)
            val header = file.inputStream().use { input -> ByteArray(16).also { input.read(it) } }
            assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray()))
        } finally {
            encrypted.close()
            context.deleteDatabase(name)
        }
    }
}
