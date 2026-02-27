package com.ytt.pos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLineItemDao {
    @Query("SELECT * FROM transaction_line_items WHERE transactionId = :transactionId ORDER BY id")
    fun observeLineItemsForTransaction(transactionId: String): Flow<List<TransactionLineItemEntity>>

    @Query("SELECT * FROM transaction_line_items")
    fun observeLineItems(): Flow<List<TransactionLineItemEntity>>

    @Insert
    suspend fun insertAll(items: List<TransactionLineItemEntity>)

    @Query("DELETE FROM transaction_line_items WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: String)
}
