package com.ytt.pos

interface PrinterGateway {
    suspend fun printReceipt(transactionId: String): Result<Unit>
    suspend fun openCashDrawer(): Result<Unit>
    suspend fun status(): PrinterStatus
}

enum class PrinterStatus {
    READY,
    BUSY,
    OFFLINE,
    ERROR,
}
