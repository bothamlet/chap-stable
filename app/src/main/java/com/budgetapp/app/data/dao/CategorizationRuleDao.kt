package com.budgetapp.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetapp.app.data.entity.CategorizationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {
    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, createdAtMillis ASC")
    fun observeRules(): Flow<List<CategorizationRuleEntity>>

    @Query("SELECT * FROM categorization_rules WHERE isEnabled = 1 ORDER BY priority DESC, createdAtMillis ASC")
    suspend fun getEnabledRules(): List<CategorizationRuleEntity>

    @Query("SELECT * FROM categorization_rules ORDER BY id ASC")
    suspend fun getRules(): List<CategorizationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: CategorizationRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rules: List<CategorizationRuleEntity>): List<Long>

    @Update
    suspend fun update(rule: CategorizationRuleEntity)

    @Delete
    suspend fun delete(rule: CategorizationRuleEntity)

    @Query("DELETE FROM categorization_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categorization_rules")
    suspend fun deleteAll()
}
