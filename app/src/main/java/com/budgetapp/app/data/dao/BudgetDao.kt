package com.budgetapp.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetapp.app.data.entity.AccountEntity
import com.budgetapp.app.data.entity.EnvelopeEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>

    @Query("SELECT * FROM envelopes WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveEnvelopes(): Flow<List<EnvelopeEntity>>

    @Query("SELECT * FROM envelopes WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getActiveEnvelopes(): List<EnvelopeEntity>

    @Query("SELECT * FROM envelopes ORDER BY id ASC")
    suspend fun getAllEnvelopes(): List<EnvelopeEntity>

    @Query(
        """
        SELECT
            envelopes.id AS id,
            envelopes.name AS name,
            envelopes.ceilingCents AS ceilingCents,
            envelopes.colorToken AS colorToken,
            envelopes.iconToken AS iconToken,
            envelopes.type AS type,
            envelopes.sortOrder AS sortOrder,
            COALESCE(
                SUM(
                    CASE
                        WHEN envelopes.type = 'goal' AND transactions.amountCents > 0 THEN transactions.amountCents
                        WHEN envelopes.type != 'goal' AND transactions.amountCents < 0 THEN -transactions.amountCents
                        ELSE 0
                    END
                ),
                0
            ) AS spentCents
        FROM envelopes
        LEFT JOIN transactions ON transactions.envelopeId = envelopes.id
            AND (
                envelopes.type = 'goal'
                OR (
                    transactions.postedAtMillis >= :periodStartMillis
                    AND transactions.postedAtMillis < :periodEndMillis
                )
            )
        WHERE envelopes.isArchived = 0
        GROUP BY envelopes.id
        ORDER BY envelopes.sortOrder ASC, envelopes.name ASC
        """,
    )
    fun observeEnvelopeBalances(
        periodStartMillis: Long,
        periodEndMillis: Long,
    ): Flow<List<EnvelopeBalance>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEnvelope(envelope: EnvelopeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEnvelopes(envelopes: List<EnvelopeEntity>): List<Long>

    @Update
    suspend fun updateEnvelope(envelope: EnvelopeEntity)

    @Query("UPDATE envelopes SET isArchived = 1, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun archiveEnvelope(id: Long, updatedAtMillis: Long)

    @Query("DELETE FROM envelopes")
    suspend fun deleteAllEnvelopes()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
