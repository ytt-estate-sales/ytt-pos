package com.ytt.pos.domain.repository

import com.ytt.pos.domain.model.Transaction
import com.ytt.pos.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(transactionId: String): Flow<Transaction?>
    suspend fun upsertTransaction(transaction: Transaction)
    suspend fun updateStatus(transactionId: String, status: TransactionStatus)
}
