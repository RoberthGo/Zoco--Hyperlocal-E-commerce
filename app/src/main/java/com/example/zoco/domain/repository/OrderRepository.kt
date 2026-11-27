package com.example.zoco.domain.repository

import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Pedidos. Igual que los productos: se guardan primero en la base local y se suben al servidor
 * cuando hay conexión ([uploadPending]).
 */
interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun getOrderById(id: Long): Order?
    suspend fun createOrder(order: Order, items: List<CartItem>): Long
    suspend fun refreshOrders(): Boolean

    /** Sube a la Web API los pedidos creados sin conexión. true si no quedó ninguno pendiente. */
    suspend fun uploadPending(): Boolean

    /** Borra los pedidos de ejemplo con nombres inventados que versiones anteriores de la app guardaron. */
    suspend fun removeSampleOrders()

    /** Borra los pedidos guardados en el teléfono para que otra cuenta no los vea. */
    suspend fun clearLocalOrders()
    suspend fun updateOrderStatus(orderId: Long, newStatus: String): Boolean
    suspend fun deleteOrder(id: Long): Boolean
}
