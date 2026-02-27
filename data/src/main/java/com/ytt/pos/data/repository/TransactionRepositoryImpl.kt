package com.ytt.pos.data.repository

import androidx.room.withTransaction
import com.ytt.pos.data.db.AppDatabase
import com.ytt.pos.data.db.TransactionDao
import com.ytt.pos.data.db.TransactionEntity
import com.ytt.pos.data.db.TransactionLineItemDao
import com.ytt.pos.data.db.TransactionLineItemEntity
import com.ytt.pos.domain.model.CartLine
import com.ytt.pos.domain.model.PermitSnapshot
import com.ytt.pos.domain.model.PrintStatus
import com.ytt.pos.domain.model.TaxStatus
import com.ytt.pos.domain.model.Transaction
import com.ytt.pos.domain.model.TransactionStatus
import com.ytt.pos.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val lineItemDao: TransactionLineItemDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        combine(transactionDao.observeTransactions(), lineItemDao.observeLineItems()) { transactions, lineItems ->
            val lineItemsByTransaction = lineItems.groupBy { it.transactionId }
            transactions.map { it.toDomain(lineItemsByTransaction[it.id].orEmpty()) }
        }

    override fun observeTransaction(transactionId: String): Flow<Transaction?> =
        combine(
            transactionDao.observeTransaction(transactionId),
            lineItemDao.observeLineItemsForTransaction(transactionId),
        ) { transaction, lineItems ->
            transaction?.toDomain(lineItems)
        }

    override suspend fun upsertTransaction(transaction: Transaction) {
        database.withTransaction {
            transactionDao.upsert(transaction.toEntity())
            lineItemDao.deleteForTransaction(transaction.id)
            lineItemDao.insertAll(transaction.lines.mapIndexed { index, line ->
                line.toEntity(transaction.id, index)
            })
        }
    }

    override suspend fun updateStatus(transactionId: String, status: TransactionStatus) {
        transactionDao.updateStatus(transactionId, status.name)
    }
}

private fun TransactionEntity.toDomain(lineItems: List<TransactionLineItemEntity>): Transaction = Transaction(
    id = id,
    createdAtEpochMs = createdAtEpochMs,
    lines = lineItems.map { it.toDomain() },
    subtotalMinor = subtotalMinor,
    taxMinor = taxMinor,
    totalMinor = totalMinor,
    taxStatus = TaxStatus.valueOf(taxStatus),
    permitSnapshot = toPermitSnapshot(),
    customerId = customerId,
    paymentId = paymentId,
    status = TransactionStatus.valueOf(status),
    printStatus = PrintStatus.valueOf(printStatus),
)

private fun TransactionLineItemEntity.toDomain(): CartLine = CartLine(
    sku = sku,
    name = name,
    unitPriceMinor = unitPriceMinor,
    qty = qty,
    discountMinor = discountMinor,
)

private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    createdAtEpochMs = createdAtEpochMs,
    subtotalMinor = subtotalMinor,
    taxMinor = taxMinor,
    totalMinor = totalMinor,
    taxStatus = taxStatus.name,
    customerId = customerId,
    paymentId = paymentId,
    status = status.name,
    printStatus = printStatus.name,
    permitBusinessName = permitSnapshot?.businessName,
    permitNumber = permitSnapshot?.permitNumber,
    permitState = permitSnapshot?.state,
    permitCapturedAtEpochMs = permitSnapshot?.capturedAtEpochMs,
)

private fun CartLine.toEntity(transactionId: String, index: Int): TransactionLineItemEntity = TransactionLineItemEntity(
    id = "$transactionId:$index:$sku",
    transactionId = transactionId,
    sku = sku,
    name = name,
    unitPriceMinor = unitPriceMinor,
    qty = qty,
    discountMinor = discountMinor,
)

private fun TransactionEntity.toPermitSnapshot(): PermitSnapshot? {
    val businessNameValue = permitBusinessName ?: return null
    val permitNumberValue = permitNumber ?: return null
    val permitStateValue = permitState ?: return null
    val capturedAtValue = permitCapturedAtEpochMs ?: return null

    return PermitSnapshot(
        businessName = businessNameValue,
        permitNumber = permitNumberValue,
        state = permitStateValue,
        capturedAtEpochMs = capturedAtValue,
    )
}
