package com.example.roznamcha.data.db.dao // Or your actual DAO package

import androidx.room.*
import com.example.roznamcha.data.db.entity.InventoryItem // Import your InventoryItem entity
import kotlinx.coroutines.flow.Flow

@Dao // <<< IMPORTANT: This annotation marks it as a DAO for Room
interface InventoryItemDao {

    /**
     * Inserts a new inventory item into the table. If an item with the same ID
     * already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.roznamcha.data.db.entity.InventoryItem)

    /**
     * Updates an existing inventory item in the table.
     * The item is identified by its primary key (id).
     */
    @Update
    suspend fun update(item: com.example.roznamcha.data.db.entity.InventoryItem)

    /**
     * Deletes an inventory item from the table.
     */
    @Delete
    suspend fun delete(item: InventoryItem)

    /**
     * Retrieves a single inventory item from the table by its ID.
     * Returns a Flow, so the UI can observe changes to this specific item.
     */
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun getItemById(id: Long): Flow<InventoryItem?>

    /**
     * Retrieves all inventory items from the table, ordered by name.
     * Returns a Flow, so the UI can observe the entire list for changes.
     */
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    /**
     * A specific query to update the quantity of an item.
     * This is more efficient than fetching the whole item, modifying it, and updating it.
     * @param itemId The ID of the item to update.
     * @param quantityChange The amount to add (for purchases) or subtract (for sales).
     */
    @Query("UPDATE inventory_items SET quantity = quantity + :quantityChange WHERE id = :itemId")
    suspend fun updateStock(itemId: Long, quantityChange: Double)
    //newly updated code

    @Query("SELECT SUM(quantity) FROM inventory_items")
    fun getTotalItemCount(): Flow<Double?>

    /**
     * Calculates the total monetary value of the entire inventory.
     * It multiplies the quantity of each item by its purchase price.
     */
    @Query("SELECT SUM(quantity * purchasePrice) FROM inventory_items")
    fun getTotalInventoryValue(): Flow<Double?>


}