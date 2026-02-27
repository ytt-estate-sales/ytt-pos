package com.ytt.pos.domain.usecase

import com.ytt.pos.domain.model.Cart
import com.ytt.pos.domain.model.CartLine
import com.ytt.pos.domain.model.Customer
import com.ytt.pos.domain.model.PermitSnapshot
import com.ytt.pos.domain.model.TaxStatus
import com.ytt.pos.domain.validation.PermitValidationError
import com.ytt.pos.domain.validation.ResalePermitValidator

class AddItemToCart {
    operator fun invoke(cart: Cart, line: CartLine): Cart {
        val existingLineIndex = cart.lines.indexOfFirst { it.sku == line.sku }
        if (existingLineIndex == -1) {
            return cart.copy(lines = cart.lines + line)
        }

        val existingLine = cart.lines[existingLineIndex]
        val updatedLine = existingLine.copy(
            qty = existingLine.qty + line.qty,
            discountMinor = existingLine.discountMinor + line.discountMinor,
        )

        return cart.copy(lines = cart.lines.toMutableList().apply { this[existingLineIndex] = updatedLine })
    }
}

class UpdateQty {
    operator fun invoke(cart: Cart, sku: String, qty: Int): Cart {
        val updatedLines = if (qty <= 0) {
            cart.lines.filterNot { it.sku == sku }
        } else {
            cart.lines.map { line ->
                if (line.sku == sku) line.copy(qty = qty) else line
            }
        }
        return cart.copy(lines = updatedLines)
    }
}

class ApplyDiscount {
    operator fun invoke(cart: Cart, sku: String, discountMinor: Long): Cart {
        val updatedLines = cart.lines.map { line ->
            if (line.sku == sku) {
                line.copy(discountMinor = discountMinor.coerceAtLeast(0))
            } else {
                line
            }
        }
        return cart.copy(lines = updatedLines)
    }
}

class AttachCustomerToCart {
    operator fun invoke(cart: Cart, customer: Customer): Cart = cart.copy(customerId = customer.id)
}

sealed class ToggleTaxExemptResaleResult {
    data class Enabled(val cart: Cart) : ToggleTaxExemptResaleResult()
    data class Disabled(val cart: Cart) : ToggleTaxExemptResaleResult()
    data class ValidationFailed(val errors: List<PermitValidationError>) : ToggleTaxExemptResaleResult()
    data object MissingCustomerOrPermit : ToggleTaxExemptResaleResult()
}

class ToggleTaxExemptResale(
    private val permitValidator: ResalePermitValidator,
    private val currentTimeMs: () -> Long = { System.currentTimeMillis() },
) {
    operator fun invoke(cart: Cart, customer: Customer?): ToggleTaxExemptResaleResult {
        return if (cart.taxStatus == TaxStatus.EXEMPT_RESALE) {
            ToggleTaxExemptResaleResult.Disabled(
                cart.copy(
                    taxStatus = TaxStatus.TAXABLE,
                    permitSnapshot = null,
                ),
            )
        } else {
            val permit = customer?.resalePermit ?: return ToggleTaxExemptResaleResult.MissingCustomerOrPermit
            val errors = permitValidator.validate(permit)
            if (errors.isNotEmpty()) {
                return ToggleTaxExemptResaleResult.ValidationFailed(errors)
            }

            val snapshot = PermitSnapshot(
                businessName = permit.businessName,
                permitNumber = permit.permitNumber,
                state = permit.state,
                capturedAtEpochMs = currentTimeMs(),
            )

            ToggleTaxExemptResaleResult.Enabled(
                cart.copy(
                    customerId = customer.id,
                    taxStatus = TaxStatus.EXEMPT_RESALE,
                    permitSnapshot = snapshot,
                ),
            )
        }
    }
}
