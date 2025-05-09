package com.example.roznamcha.data // Or your repository package

import com.example.roznamcha.data.db.dao.CustomerDao
import com.example.roznamcha.data.db.entity.Customer
import kotlinx.coroutines.flow.Flow

// Repository for Customer related operations
class CustomerRepository(private val customerDao: CustomerDao) {

    fun getCustomerById(id: Long): Flow<Customer?> {
        return customerDao.getCustomerById(id)
    }

    fun getAllActiveCustomers(): Flow<List<Customer>> {
        return customerDao.getAllActiveCustomers()
    }

    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers()
    }


    fun searchActiveCustomers(query: String): Flow<List<Customer>> {
        // Prepare pattern for LIKE - check if DAO handles % or needs it passed
        // Assuming DAO handles it like: LIKE '%' || :query || '%'
        return customerDao.searchActiveCustomers(query)
    }

    suspend fun insert(customer: Customer) {
        customerDao.insert(customer)
    }

    suspend fun update(customer: Customer) {
        customerDao.update(customer)
    }

    suspend fun delete(customer: Customer) {
        customerDao.delete(customer)
    }

}