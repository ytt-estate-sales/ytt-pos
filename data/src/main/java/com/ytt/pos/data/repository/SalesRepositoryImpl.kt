package com.ytt.pos.data.repository

import com.ytt.pos.data.db.SaleDao
import com.ytt.pos.domain.repository.SalesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
) : SalesRepository {
    override fun pendingSaleCount(): Flow<Int> = saleDao.observePendingSaleCount()

    override suspend fun markAllAsSynced() {
        saleDao.markAllSynced()
    }
}
