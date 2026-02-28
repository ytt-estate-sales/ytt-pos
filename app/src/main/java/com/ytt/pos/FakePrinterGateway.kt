package com.ytt.pos

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakePrinterGateway @Inject constructor() : PrinterGateway {
    override suspend fun printReceipt(transactionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun openCashDrawer(): Result<Unit> = Result.success(Unit)

    override suspend fun status(): PrinterStatus = PrinterStatus.READY
}
