package com.ytt.pos.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    override val selectedPrinterId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SELECTED_PRINTER_ID]
    }

    override val selectedReaderId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SELECTED_READER_ID]
    }

    override val drawerConnected: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DRAWER_CONNECTED] ?: false
    }

    override suspend fun setSelectedPrinterId(printerId: String?) {
        dataStore.edit { prefs ->
            if (printerId == null) prefs.remove(SELECTED_PRINTER_ID) else prefs[SELECTED_PRINTER_ID] = printerId
        }
    }

    override suspend fun setSelectedReaderId(readerId: String?) {
        dataStore.edit { prefs ->
            if (readerId == null) prefs.remove(SELECTED_READER_ID) else prefs[SELECTED_READER_ID] = readerId
        }
    }

    override suspend fun setDrawerConnected(connected: Boolean) {
        dataStore.edit { prefs ->
            prefs[DRAWER_CONNECTED] = connected
        }
    }

    private companion object {
        val SELECTED_PRINTER_ID = stringPreferencesKey("selectedPrinterId")
        val SELECTED_READER_ID = stringPreferencesKey("selectedReaderId")
        val DRAWER_CONNECTED = booleanPreferencesKey("drawerConnected")
    }
}
