package com.example.roznamcha.data.db.dao // Or your package

import androidx.room.*
import com.example.roznamcha.data.db.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer) // Needed by repository

    // Gets ONLY active customers (e.g., for selection dropdowns)
    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCustomers(): Flow<List<Customer>>

    // <<< ENSURE THIS EXISTS: Gets ALL customers (for management list) >>>
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>
    //udpate: retrieve single cutomer by their ID
    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Long): Flow<Customer?>

    // Example search (adjust LIKE pattern if needed)
    @Query("SELECT * FROM customers WHERE isActive = 1 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchActiveCustomers(query: String): Flow<List<Customer>>
    //udpate customer idea
    @Query("SELECT COUNT(id) FROM customers WHERE isActive = 1")
    fun getActiveCustomerCount(): Flow<Int>

}