package com.ytt.pos.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs WHERE status = :status ORDER BY createdAtEpochMs")
    fun observeJobsByStatus(status: String): Flow<List<PrintJobEntity>>

    @Upsert
    suspend fun upsert(job: PrintJobEntity)
}
