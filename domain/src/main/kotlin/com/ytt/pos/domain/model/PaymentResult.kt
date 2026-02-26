package com.ytt.pos.domain.model

data class PaymentResult(
    val approved: Boolean,
    val reference: String,
)
