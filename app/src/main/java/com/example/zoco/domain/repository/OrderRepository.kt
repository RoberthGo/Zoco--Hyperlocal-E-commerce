package com.example.zoco.domain.repository

import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Pedidos. Igual que los productos: se guardan primero en la base local y se suben al servidor
 * cuando hay conexión ([pushPending]).
 */
interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun getOrderById(id: Long): Order?
    suspend fun createOrder(order: Order, items: List<CartItem>): Long
    suspend fun syncOrdersFromRemote(): Boolean

    /** Sube a la Web API los pedidos creados sin conexión. true si no quedó ninguno pendiente. */
    suspend fun pushPending(): Boolean
    suspend fun populateInitialOrdersIfEmpty()
    suspend fun updateOrderStatus(orderId: Long, newStatus: String): Boolean
    suspend fun deleteOrder(id: Long): Boolean
}
