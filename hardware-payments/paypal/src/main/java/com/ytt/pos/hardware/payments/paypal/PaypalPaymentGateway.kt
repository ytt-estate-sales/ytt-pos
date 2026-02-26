package com.ytt.pos.hardware.payments.paypal

import com.ytt.pos.domain.hardware.PaymentGateway

class PaypalPaymentGateway @javax.inject.Inject constructor() : PaymentGateway {
    override suspend fun charge(amountCents: Long): Boolean = amountCents > 0
}
