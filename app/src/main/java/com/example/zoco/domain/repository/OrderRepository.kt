package com.example.zoco.domain.repository

import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun getOrderById(id: Long): Order?
    suspend fun createOrder(order: Order, items: List<CartItem>): Long
    suspend fun syncOrdersFromRemote(): Boolean
    suspend fun populateInitialOrdersIfEmpty()
    suspend fun deleteOrder(id: Long): Boolean
}
