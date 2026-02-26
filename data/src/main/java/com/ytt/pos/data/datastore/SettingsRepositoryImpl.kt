package com.ytt.pos.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ytt.pos.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val storeName: Flow<String> = dataStore.data.map { prefs ->
        prefs[STORE_NAME_KEY].orEmpty()
    }

    override suspend fun setStoreName(name: String) {
        dataStore.edit { prefs ->
            prefs[STORE_NAME_KEY] = name
        }
    }

    private companion object {
        val STORE_NAME_KEY = stringPreferencesKey("store_name")
    }
}
