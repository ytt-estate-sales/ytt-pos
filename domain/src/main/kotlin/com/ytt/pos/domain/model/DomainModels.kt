package com.ytt.pos.domain.model

data class Money(
    val amountMinor: Long,
    val currency: String,
)

data class CartLine(
    val sku: String,
    val name: String,
    val unitPriceMinor: Long,
    val qty: Int,
    val discountMinor: Long = 0,
) {
    val lineSubtotalMinor: Long
        get() = unitPriceMinor * qty

    val lineTotalMinor: Long
        get() = (lineSubtotalMinor - discountMinor).coerceAtLeast(0)
}

data class Cart(
    val id: String,
    val lines: List<CartLine> = emptyList(),
    val customerId: String? = null,
    val taxStatus: TaxStatus = TaxStatus.TAXABLE,
    val permitSnapshot: PermitSnapshot? = null,
)

data class Customer(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val resalePermit: ResalePermit? = null,
)

data class ResalePermit(
    val businessName: String,
    val permitNumber: String,
    val state: String,
    val expiresOn: String? = null,
)

data class PermitSnapshot(
    val businessName: String,
    val permitNumber: String,
    val state: String,
    val capturedAtEpochMs: Long,
)

data class Payment(
    val id: String,
    val method: PaymentMethod,
    val amountMinor: Long,
    val currency: String,
    val providerRef: String? = null,
    val status: TransactionStatus,
)

data class Transaction(
    val id: String,
    val createdAtEpochMs: Long,
    val lines: List<CartLine>,
    val subtotalMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val taxStatus: TaxStatus,
    val permitSnapshot: PermitSnapshot? = null,
    val customerId: String? = null,
    val paymentId: String? = null,
    val status: TransactionStatus,
    val printStatus: PrintStatus,
)

enum class TaxStatus {
    TAXABLE,
    EXEMPT_RESALE,
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    SYNC_PENDING,
    SYNCED,
    FAILED,
}

enum class PaymentMethod {
    CASH,
    CARD,
}

enum class PrintStatus {
    NOT_PRINTED,
    PRINTED,
    SKIPPED_BY_MANAGER,
    FAILED,
}
