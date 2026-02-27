package com.ytt.pos.data.repository

import com.ytt.pos.data.db.CustomerDao
import com.ytt.pos.data.db.CustomerEntity
import com.ytt.pos.data.db.ResalePermitDao
import com.ytt.pos.data.db.ResalePermitEntity
import com.ytt.pos.domain.model.Customer
import com.ytt.pos.domain.model.ResalePermit
import com.ytt.pos.domain.repository.CustomerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val resalePermitDao: ResalePermitDao,
) : CustomerRepository {

    override fun observeCustomers(): Flow<List<Customer>> =
        combine(customerDao.observeCustomers(), resalePermitDao.observePermits()) { customers, permits ->
            val permitByCustomerId = permits.associateBy { it.customerId }
            customers.map { it.toDomain(permitByCustomerId[it.id]) }
        }

    override fun observeCustomer(customerId: String): Flow<Customer?> =
        combine(
            customerDao.observeCustomer(customerId),
            resalePermitDao.observePermitForCustomer(customerId),
        ) { customer, permit -> customer?.toDomain(permit) }

    override suspend fun upsertCustomer(customer: Customer) {
        customerDao.upsert(customer.toEntity())
        val permit = customer.resalePermit
        if (permit == null) {
            resalePermitDao.deleteByCustomerId(customer.id)
        } else {
            resalePermitDao.upsert(permit.toEntity(customer.id))
        }
    }

    override suspend fun deleteCustomer(customerId: String) {
        customerDao.deleteById(customerId)
    }
}

private fun CustomerEntity.toDomain(permit: ResalePermitEntity?): Customer = Customer(
    id = id,
    name = name,
    email = email,
    phone = phone,
    resalePermit = permit?.toDomain(),
)

private fun ResalePermitEntity.toDomain(): ResalePermit = ResalePermit(
    businessName = businessName,
    permitNumber = permitNumber,
    state = state,
    expiresOn = expiresOn,
)

private fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
)

private fun ResalePermit.toEntity(customerId: String): ResalePermitEntity = ResalePermitEntity(
    customerId = customerId,
    businessName = businessName,
    permitNumber = permitNumber,
    state = state,
    expiresOn = expiresOn,
)
