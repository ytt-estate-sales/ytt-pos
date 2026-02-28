package com.ytt.pos.hardware.printer.star

import com.ytt.pos.domain.receipt.ReceiptContent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarReceiptTextRenderer @Inject constructor() {
    fun render(receipt: ReceiptContent): String {
        val body = receipt.lines.joinToString(separator = "\n")
        // TODO: Build StarPRNT commands for alignment, paper cutting, and code pages.
        return "$body\n\n\n"
    }
}
