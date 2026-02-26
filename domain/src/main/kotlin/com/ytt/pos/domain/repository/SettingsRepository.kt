package com.ytt.pos.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val storeName: Flow<String>
    suspend fun setStoreName(name: String)
}
