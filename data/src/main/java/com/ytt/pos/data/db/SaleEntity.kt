package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalCents: Long,
    val synced: Boolean = false,
)
