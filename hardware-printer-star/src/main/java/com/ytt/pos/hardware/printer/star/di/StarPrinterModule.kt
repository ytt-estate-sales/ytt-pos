package com.ytt.pos.hardware.printer.star.di

import com.ytt.pos.hardware.printer.star.StarPrinterService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StarPrinterModule {
    @Provides
    @Singleton
    fun provideStarPrinterService(service: StarPrinterService): StarPrinterService = service
}
