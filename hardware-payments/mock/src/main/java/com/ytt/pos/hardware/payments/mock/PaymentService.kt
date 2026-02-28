package com.ytt.pos.hardware.payments.mock

interface PaymentService {
    suspend fun startCardPayment(
        totalMinor: Long,
        currency: String,
        receiptId: String,
    ): PaymentResult
}

sealed interface PaymentResult {
    data class Approved(val providerRef: String) : PaymentResult
    data class Declined(val reason: String = "Card was declined") : PaymentResult
    data class Cancelled(val message: String = "Card payment cancelled") : PaymentResult
    data class Failed(val error: String = "Card reader failed") : PaymentResult
}

class MockPaymentService @javax.inject.Inject constructor() : PaymentService {
    override suspend fun startCardPayment(
        totalMinor: Long,
        currency: String,
        receiptId: String,
    ): PaymentResult {
        if (totalMinor <= 0) return PaymentResult.Failed("Invalid amount")
        return PaymentResult.Approved(providerRef = "mock-$currency-$receiptId")
    }
}
