package com.ytt.pos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        ResalePermitEntity::class,
        TransactionEntity::class,
        TransactionLineItemEntity::class,
        PaymentEntity::class,
        PrintJobEntity::class,
        SaleEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun resalePermitDao(): ResalePermitDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLineItemDao(): TransactionLineItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun saleDao(): SaleDao
}
