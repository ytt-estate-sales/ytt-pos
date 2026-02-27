package com.ytt.pos.domain.repository

import com.ytt.pos.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeCustomers(): Flow<List<Customer>>
    fun observeCustomer(customerId: String): Flow<Customer?>
    suspend fun upsertCustomer(customer: Customer)
    suspend fun deleteCustomer(customerId: String)
}
