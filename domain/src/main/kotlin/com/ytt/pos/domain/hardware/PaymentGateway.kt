package com.ytt.pos.domain.hardware

interface PaymentGateway {
    suspend fun charge(amountCents: Long): Boolean
}
