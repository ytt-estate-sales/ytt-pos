package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [Index("transactionId")],
)
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String?,
    val method: String,
    val amountMinor: Long,
    val currency: String,
    val providerRef: String?,
    val status: String,
    val createdAtEpochMs: Long,
)
