package com.ytt.pos.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ytt.pos.domain.repository.SalesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val salesRepository: SalesRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        salesRepository.markAllAsSynced()
        return Result.success()
    }
}
