package com.ytt.pos.di

import com.ytt.pos.domain.hardware.PaymentGateway
import com.ytt.pos.hardware.payments.mock.MockPaymentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindPaymentGateway(impl: MockPaymentGateway): PaymentGateway
}
