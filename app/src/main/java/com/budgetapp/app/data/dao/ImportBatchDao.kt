package com.budgetapp.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetapp.app.data.entity.ImportBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY importedAtMillis DESC")
    fun observeImportBatches(): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM import_batches ORDER BY id ASC")
    suspend fun getImportBatches(): List<ImportBatchEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(importBatch: ImportBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(importBatches: List<ImportBatchEntity>): List<Long>

    @Query("DELETE FROM import_batches")
    suspend fun deleteAll()
}
