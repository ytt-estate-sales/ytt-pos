package com.ytt.pos.data.repository

import com.ytt.pos.data.db.PaymentDao
import com.ytt.pos.data.db.PaymentEntity
import com.ytt.pos.domain.model.Payment
import com.ytt.pos.domain.model.PaymentMethod
import com.ytt.pos.domain.model.TransactionStatus
import com.ytt.pos.domain.repository.PaymentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
) : PaymentRepository {
    override fun observePayment(paymentId: String): Flow<Payment?> =
        paymentDao.observePayment(paymentId).map { it?.toDomain() }

    override fun observePayments(transactionId: String): Flow<List<Payment>> =
        paymentDao.observePayments(transactionId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertPayment(payment: Payment, transactionId: String?) {
        paymentDao.upsert(payment.toEntity(transactionId))
    }
}

private fun PaymentEntity.toDomain(): Payment = Payment(
    id = id,
    method = PaymentMethod.valueOf(method),
    amountMinor = amountMinor,
    currency = currency,
    providerRef = providerRef,
    status = TransactionStatus.valueOf(status),
)

private fun Payment.toEntity(transactionId: String?): PaymentEntity = PaymentEntity(
    id = id,
    transactionId = transactionId,
    method = method.name,
    amountMinor = amountMinor,
    currency = currency,
    providerRef = providerRef,
    status = status.name,
    createdAtEpochMs = System.currentTimeMillis(),
)
