package com.budgetapp.app.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetapp.app.data.dao.BudgetDao
import com.budgetapp.app.data.dao.CategorizationRuleDao
import com.budgetapp.app.data.dao.ImportBatchDao
import com.budgetapp.app.data.dao.TransactionDao
import com.budgetapp.app.data.entity.AccountEntity
import com.budgetapp.app.data.entity.CategorizationRuleEntity
import com.budgetapp.app.data.entity.EnvelopeEntity
import com.budgetapp.app.data.entity.ImportBatchEntity
import com.budgetapp.app.data.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        EnvelopeEntity::class,
        TransactionEntity::class,
        CategorizationRuleEntity::class,
        ImportBatchEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categorizationRuleDao(): CategorizationRuleDao

    abstract fun importBatchDao(): ImportBatchDao

    companion object {
        val Migration2To3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE envelopes ADD COLUMN type TEXT NOT NULL DEFAULT 'variable'")
                    db.execSQL("UPDATE envelopes SET type = 'fixed' WHERE lower(colorToken) = 'fixed'")
                }
            }

        val Migration3To4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE envelopes ADD COLUMN iconToken TEXT NOT NULL DEFAULT 'wallet'")
                }
            }
    }
}
