package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val createdAtEpochMs: Long,
    val subtotalMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val taxStatus: String,
    val customerId: String?,
    val paymentId: String?,
    val status: String,
    val printStatus: String,
    val permitBusinessName: String?,
    val permitNumber: String?,
    val permitState: String?,
    val permitCapturedAtEpochMs: Long?,
)
