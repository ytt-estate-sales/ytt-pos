package com.ytt.pos.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.ytt.pos.data.datastore.SettingsRepositoryImpl
import com.ytt.pos.data.db.AppDatabase
import com.ytt.pos.data.db.CustomerDao
import com.ytt.pos.data.db.PaymentDao
import com.ytt.pos.data.db.PrintJobDao
import com.ytt.pos.data.db.ResalePermitDao
import com.ytt.pos.data.db.SaleDao
import com.ytt.pos.data.db.TransactionDao
import com.ytt.pos.data.db.TransactionLineItemDao
import com.ytt.pos.data.repository.CustomerRepositoryImpl
import com.ytt.pos.data.repository.PaymentRepositoryImpl
import com.ytt.pos.data.repository.SalesRepositoryImpl
import com.ytt.pos.data.repository.TransactionRepositoryImpl
import com.ytt.pos.domain.repository.CustomerRepository
import com.ytt.pos.domain.repository.PaymentRepository
import com.ytt.pos.domain.repository.SalesRepository
import com.ytt.pos.domain.repository.SettingsRepository
import com.ytt.pos.domain.repository.TransactionRepository
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

    @Binds
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pos.db").build()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideResalePermitDao(db: AppDatabase): ResalePermitDao = db.resalePermitDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideTransactionLineItemDao(db: AppDatabase): TransactionLineItemDao = db.transactionLineItemDao()

    @Provides
    fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun providePrintJobDao(db: AppDatabase): PrintJobDao = db.printJobDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

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
