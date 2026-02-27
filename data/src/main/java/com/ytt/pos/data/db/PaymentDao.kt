package com.ytt.pos.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE id = :paymentId LIMIT 1")
    fun observePayment(paymentId: String): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments WHERE transactionId = :transactionId ORDER BY createdAtEpochMs")
    fun observePayments(transactionId: String): Flow<List<PaymentEntity>>

    @Upsert
    suspend fun upsert(payment: PaymentEntity)
}
