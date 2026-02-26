package com.ytt.pos.hardware.payments.mock

import com.ytt.pos.domain.hardware.PaymentGateway

class MockPaymentGateway @javax.inject.Inject constructor() : PaymentGateway {
    override suspend fun charge(amountCents: Long): Boolean = true
}
