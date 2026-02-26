package com.ytt.pos.domain.repository

import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    fun pendingSaleCount(): Flow<Int>
    suspend fun markAllAsSynced()
}
