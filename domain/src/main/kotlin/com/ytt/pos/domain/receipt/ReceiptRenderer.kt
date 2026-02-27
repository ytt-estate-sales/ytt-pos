package com.ytt.pos.domain.receipt

import com.ytt.pos.domain.model.PermitSnapshot
import com.ytt.pos.domain.model.TaxStatus

data class ReceiptLine(
    val name: String,
    val qty: Int,
    val unitPriceMinor: Long,
    val discountMinor: Long = 0,
)

data class ReceiptModel(
    val transactionId: String,
    val lines: List<ReceiptLine>,
    val subtotalMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val taxStatus: TaxStatus,
    val permitSnapshot: PermitSnapshot? = null,
    val currency: String = "USD",
)

data class ReceiptContent(
    val lines: List<String>,
)

class ReceiptRenderer {
    fun render(model: ReceiptModel): ReceiptContent {
        val output = mutableListOf<String>()
        output += "TRANSACTION ${model.transactionId}"
        output += ""

        model.lines.forEach { line ->
            output += "${line.qty} x ${line.name}  ${formatMinor(line.unitPriceMinor, model.currency)}"
            if (line.discountMinor > 0) {
                output += "  Discount  -${formatMinor(line.discountMinor, model.currency)}"
            }
        }

        output += ""
        output += "Subtotal: ${formatMinor(model.subtotalMinor, model.currency)}"
        output += "Tax: ${formatMinor(model.taxMinor, model.currency)}"
        output += "Total: ${formatMinor(model.totalMinor, model.currency)}"

        if (model.taxStatus == TaxStatus.EXEMPT_RESALE) {
            val permitSnapshot = model.permitSnapshot
            if (permitSnapshot != null) {
                output += ""
                output += "TAX EXEMPT - RESALE"
                output += permitSnapshot.businessName
                output += "Permit # ${permitSnapshot.permitNumber}  ${permitSnapshot.state}"
            }
        }

        return ReceiptContent(lines = output)
    }

    private fun formatMinor(amountMinor: Long, currency: String): String {
        val major = amountMinor / 100
        val minor = kotlin.math.abs(amountMinor % 100)
        return "$currency $major.${minor.toString().padStart(2, '0')}"
    }
}
