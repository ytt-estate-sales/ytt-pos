package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_line_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId")],
)
data class TransactionLineItemEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val sku: String,
    val name: String,
    val unitPriceMinor: Long,
    val qty: Int,
    val discountMinor: Long,
)
