package com.ytt.pos.di

import com.ytt.pos.HardcodedManagerAuthService
import com.ytt.pos.HardwareManager
import com.ytt.pos.ManagerAuthService
import com.ytt.pos.AndroidNetworkMonitor
import com.ytt.pos.NetworkMonitor
import com.ytt.pos.PrinterGateway
import com.ytt.pos.domain.hardware.PaymentGateway
import com.ytt.pos.domain.usecase.AddItemToCart
import com.ytt.pos.domain.usecase.ApplyDiscount
import com.ytt.pos.domain.usecase.ToggleTaxExemptResale
import com.ytt.pos.domain.usecase.UpdateQty
import com.ytt.pos.domain.validation.ResalePermitValidator
import com.ytt.pos.hardware.payments.mock.MockPaymentGateway
import com.ytt.pos.hardware.payments.mock.MockPaymentService
import com.ytt.pos.hardware.payments.mock.PaymentService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindPaymentGateway(impl: MockPaymentGateway): PaymentGateway

    @Binds
    @Singleton
    abstract fun bindPrinterGateway(impl: HardwareManager): PrinterGateway

    @Binds
    @Singleton
    abstract fun bindManagerAuthService(impl: HardcodedManagerAuthService): ManagerAuthService

    @Binds
    @Singleton
    abstract fun bindPaymentService(impl: MockPaymentService): PaymentService

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideResalePermitValidator(): ResalePermitValidator = ResalePermitValidator()
    @Provides
    fun provideAddItemToCart(): AddItemToCart = AddItemToCart()

    @Provides
    fun provideUpdateQty(): UpdateQty = UpdateQty()

    @Provides
    fun provideApplyDiscount(): ApplyDiscount = ApplyDiscount()

    @Provides
    fun provideToggleTaxExemptResale(
        resalePermitValidator: ResalePermitValidator,
    ): ToggleTaxExemptResale = ToggleTaxExemptResale(resalePermitValidator)
}
