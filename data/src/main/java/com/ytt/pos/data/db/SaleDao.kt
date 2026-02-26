package com.ytt.pos.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT COUNT(*) FROM sales WHERE synced = 0")
    fun observePendingSaleCount(): Flow<Int>

    @Query("UPDATE sales SET synced = 1 WHERE synced = 0")
    suspend fun markAllSynced()
}
