package com.example.zoco.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isRemote = 0 ORDER BY createdAt DESC")
    fun getLocalOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity): Int

    @Delete
    suspend fun deleteOrder(order: OrderEntity): Int

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long): Int

    @Query("SELECT * FROM orders WHERE pendingSync = 1")
    suspend fun getPendingOrders(): List<OrderEntity>

    /** Borra todos los pedidos guardados en el teléfono (al cerrar sesión). */
    @Query("DELETE FROM orders")
    suspend fun clearAll(): Int

    /** Borra los 2 pedidos de ejemplo (ids 101 y 102, estado LOCAL_STORED) que guardaban versiones anteriores. */
    @Query("DELETE FROM orders WHERE id IN (101, 102) AND status = 'LOCAL_STORED' AND isRemote = 0 AND pendingSync = 0")
    suspend fun deleteOldSampleOrders(): Int

    /** Borra los pedidos descargados de la API, sin tocar los que aún esperan subirse. */
    @Query("DELETE FROM orders WHERE pendingSync = 0 AND (isRemote = 1 OR customerName LIKE '[En Línea]%')")
    suspend fun deleteRemoteOrders(): Int
}
