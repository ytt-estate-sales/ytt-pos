package com.ytt.pos

import com.ytt.pos.domain.model.CartLine
import com.ytt.pos.domain.model.TaxStatus

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val subtotalMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val taxStatus: TaxStatus = TaxStatus.TAXABLE,
    val customerName: String? = null,
    val message: String? = null,
)

sealed class CartEvent {
    data object AddTestItem : CartEvent()
    data object ToggleTaxExemption : CartEvent()
    data class UpdateLineQty(val sku: String, val qty: Int) : CartEvent()
    data class ApplyLineDiscount(val sku: String, val discountMinor: Long) : CartEvent()
}
