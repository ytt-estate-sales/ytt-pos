package com.ytt.pos.domain.repository

import com.ytt.pos.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun observePayment(paymentId: String): Flow<Payment?>
    fun observePayments(transactionId: String): Flow<List<Payment>>
    suspend fun upsertPayment(payment: Payment, transactionId: String?)
}
