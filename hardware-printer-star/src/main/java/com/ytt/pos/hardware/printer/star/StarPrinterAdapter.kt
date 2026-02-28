package com.ytt.pos.hardware.printer.star

import com.ytt.pos.domain.receipt.ReceiptContent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarPrinterAdapter @Inject constructor(
    private val service: StarPrinterService,
) {
    fun printTransaction(transactionId: String): Result<Unit> {
        val receipt = ReceiptContent(lines = listOf("TRANSACTION $transactionId"))
        return service.printReceipt(receipt)
    }
}
