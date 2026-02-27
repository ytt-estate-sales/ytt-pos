package com.ytt.pos.data.repository

import com.ytt.pos.domain.model.Cart
import com.ytt.pos.domain.repository.CartRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class CartRepositoryImpl @Inject constructor() : CartRepository {
    private val cartFlow = MutableStateFlow(Cart(id = "active-cart"))

    override fun observeCart(): Flow<Cart> = cartFlow

    override suspend fun updateCart(cart: Cart) {
        cartFlow.value = cart
    }
}
