package com.example.zoco.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isRemote = 0 ORDER BY name ASC")
    fun getLocalProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long): Int

    @Query("SELECT * FROM products WHERE pendingSync = 1")
    suspend fun getPendingProducts(): List<ProductEntity>

    /** Borra los productos descargados de la API, sin tocar los que aún esperan subirse. */
    @Query("DELETE FROM products WHERE pendingSync = 0 AND isRemote = 1")
    suspend fun deleteRemoteProducts(): Int
}
