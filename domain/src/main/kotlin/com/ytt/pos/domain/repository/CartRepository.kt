package com.ytt.pos.domain.repository

import com.ytt.pos.domain.model.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun updateCart(cart: Cart)
}
