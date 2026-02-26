package com.ytt.pos.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.work.WorkManager
import com.ytt.pos.data.datastore.SettingsRepositoryImpl
import com.ytt.pos.data.db.PosDatabase
import com.ytt.pos.data.db.SaleDao
import com.ytt.pos.data.repository.SalesRepositoryImpl
import com.ytt.pos.domain.repository.SalesRepository
import com.ytt.pos.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    abstract fun bindSalesRepository(impl: SalesRepositoryImpl): SalesRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PosDatabase =
        Room.databaseBuilder(context, PosDatabase::class.java, "pos.db").build()

    @Provides
    fun provideSaleDao(db: PosDatabase): SaleDao = db.saleDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings.preferences_pb") },
        )

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
