package com.ytt.pos.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resale_permits",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("customerId")],
)
data class ResalePermitEntity(
    @PrimaryKey
    val customerId: String,
    val businessName: String,
    val permitNumber: String,
    val state: String,
    val expiresOn: String?,
)
