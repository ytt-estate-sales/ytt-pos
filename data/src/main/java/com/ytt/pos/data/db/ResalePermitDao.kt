package com.ytt.pos.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ResalePermitDao {
    @Query("SELECT * FROM resale_permits WHERE customerId = :customerId LIMIT 1")
    fun observePermitForCustomer(customerId: String): Flow<ResalePermitEntity?>

    @Query("SELECT * FROM resale_permits")
    fun observePermits(): Flow<List<ResalePermitEntity>>

    @Upsert
    suspend fun upsert(permit: ResalePermitEntity)

    @Query("DELETE FROM resale_permits WHERE customerId = :customerId")
    suspend fun deleteByCustomerId(customerId: String)
}
