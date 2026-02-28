package com.ytt.pos

fun formatMinor(amountMinor: Long): String {
    val dollars = amountMinor / 100
    val cents = (amountMinor % 100).toString().padStart(2, '0')
    return "$$dollars.$cents"
}
