package com.ytt.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytt.pos.domain.model.Cart
import com.ytt.pos.domain.model.CartLine
import com.ytt.pos.domain.model.Customer
import com.ytt.pos.domain.model.TaxStatus
import com.ytt.pos.domain.repository.CartRepository
import com.ytt.pos.domain.repository.CustomerRepository
import com.ytt.pos.domain.usecase.AddItemToCart
import com.ytt.pos.domain.usecase.ApplyDiscount
import com.ytt.pos.domain.usecase.ToggleTaxExemptResale
import com.ytt.pos.domain.usecase.ToggleTaxExemptResaleResult
import com.ytt.pos.domain.usecase.UpdateQty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAX_RATE = 0.0825

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val customerRepository: CustomerRepository,
    private val toggleTaxExemptResale: ToggleTaxExemptResale,
    private val addItemToCart: AddItemToCart,
    private val updateQty: UpdateQty,
    private val applyDiscount: ApplyDiscount,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    private val cartState = cartRepository.observeCart().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Cart(id = "active-cart"),
    )

    private val customerState = cartState
        .flatMapLatest { cart ->
            cart.customerId?.let { customerRepository.observeCustomer(it) } ?: flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<CartUiState> = combine(
        cartState,
        customerState,
        messageFlow,
    ) { cart, customer, message ->
        cart.toUiState(customer = customer, message = message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState(),
    )

    fun onEvent(event: CartEvent) {
        when (event) {
            CartEvent.AddTestItem -> addTestItem()
            CartEvent.ToggleTaxExemption -> toggleTaxExemption()
            is CartEvent.UpdateLineQty -> changeQty(event.sku, event.qty)
            is CartEvent.ApplyLineDiscount -> changeDiscount(event.sku, event.discountMinor)
        }
    }

    private fun addTestItem() {
        viewModelScope.launch {
            val updated = addItemToCart(
                cart = cartState.value,
                line = CartLine(
                    sku = "TEST-SKU",
                    name = "Test Item",
                    unitPriceMinor = 599,
                    qty = 1,
                ),
            )
            cartRepository.updateCart(updated)
            messageFlow.value = null
        }
    }

    private fun toggleTaxExemption() {
        viewModelScope.launch {
            when (val result = toggleTaxExemptResale(cartState.value, customerState.value)) {
                is ToggleTaxExemptResaleResult.Enabled -> {
                    cartRepository.updateCart(result.cart)
                    messageFlow.value = "Tax exempt resale enabled"
                }

                is ToggleTaxExemptResaleResult.Disabled -> {
                    cartRepository.updateCart(result.cart)
                    messageFlow.value = "Tax exempt resale disabled"
                }

                ToggleTaxExemptResaleResult.MissingCustomerOrPermit -> {
                    messageFlow.value = "Attach a customer with a valid permit first"
                }

                is ToggleTaxExemptResaleResult.ValidationFailed -> {
                    messageFlow.value = "Permit validation failed"
                }
            }
        }
    }

    private fun changeQty(sku: String, qty: Int) {
        viewModelScope.launch {
            val updated = updateQty(cartState.value, sku, qty)
            cartRepository.updateCart(updated)
        }
    }

    private fun changeDiscount(sku: String, discountMinor: Long) {
        viewModelScope.launch {
            val updated = applyDiscount(cartState.value, sku, discountMinor)
            cartRepository.updateCart(updated)
        }
    }
}

private fun Cart.toUiState(customer: Customer?, message: String?): CartUiState {
    val subtotal = lines.sumOf { it.lineTotalMinor }
    val tax = if (taxStatus == TaxStatus.EXEMPT_RESALE) 0 else (subtotal * TAX_RATE).toLong()
    return CartUiState(
        lines = lines,
        subtotalMinor = subtotal,
        taxMinor = tax,
        totalMinor = subtotal + tax,
        taxStatus = taxStatus,
        customerName = customer?.name,
        message = message,
    )
}
