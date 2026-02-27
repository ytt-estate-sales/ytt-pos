package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "print_jobs",
    indices = [Index("transactionId")],
)
data class PrintJobEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
