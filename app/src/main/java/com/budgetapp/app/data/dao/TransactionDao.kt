package com.budgetapp.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.budgetapp.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY postedAtMillis DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE envelopeId = :envelopeId
            AND postedAtMillis >= :periodStartMillis
            AND postedAtMillis < :periodEndMillis
        ORDER BY postedAtMillis DESC, id DESC
        """,
    )
    fun observeTransactionsForEnvelopeInPeriod(
        envelopeId: Long,
        periodStartMillis: Long,
        periodEndMillis: Long,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE envelopeId IS NULL ORDER BY postedAtMillis DESC, id DESC")
    fun observeInbox(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getTransactions(): List<TransactionEntity>

    @Query("SELECT externalId FROM transactions WHERE externalId IS NOT NULL")
    suspend fun getExternalIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE transactions
        SET envelopeId = :envelopeId, status = :status, updatedAtMillis = :updatedAtMillis
        WHERE id = :transactionId
        """,
    )
    suspend fun assignEnvelope(
        transactionId: Long,
        envelopeId: Long,
        status: String,
        updatedAtMillis: Long,
    )

    @Transaction
    suspend fun insertCountingAccepted(transactions: List<TransactionEntity>): Int {
        return insertAll(transactions).count { it != -1L }
    }
}
