package com.ytt.pos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SaleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun saleDao(): SaleDao
}
