package com.ytt.pos.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val selectedPrinterId: Flow<String?>
    val selectedReaderId: Flow<String?>
    val drawerConnected: Flow<Boolean>

    suspend fun setSelectedPrinterId(printerId: String?)
    suspend fun setSelectedReaderId(readerId: String?)
    suspend fun setDrawerConnected(connected: Boolean)
}
