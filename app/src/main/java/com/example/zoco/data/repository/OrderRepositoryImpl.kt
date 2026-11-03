package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.local.OrderDao
import com.example.zoco.data.local.OrderEntity
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.OrderApiService
import com.example.zoco.data.remote.dto.OrderDto
import com.example.zoco.data.remote.dto.OrderItemDto
import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepositoryImpl(
    private val orderDao: OrderDao,
    private val orderApiService: OrderApiService = RetrofitClient.orderApiService
) : OrderRepository {

    companion object {
        private const val TAG = "OrderRepository"
    }

    override fun getOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getOrderById(id: Long): Order? {
        return orderDao.getOrderById(id)?.toDomain()
    }

    override suspend fun createOrder(order: Order, items: List<CartItem>): Long {
        // 1. Generar resumen textual de productos para persistencia local inmediata
        val summary = items.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
        val localOrder = order.copy(itemSummary = summary)

        // 2. Transacción en base de datos local SQLite (Room)
        val localId = orderDao.insertOrder(OrderEntity.fromDomain(localOrder))

        // 3. Sincronización Web API (Retrofit POST /api/v1/orders)
        try {
            val orderDto = OrderDto(
                customerName = order.customerName.ifBlank { "Comprador Zoco" },
                customerPhone = order.customerPhone,
                deliveryAddress = order.deliveryAddress,
                scheduledTime = order.scheduledTime,
                totalAmount = order.totalAmount,
                status = order.status,
                isExpress = order.isExpress,
                items = items.map {
                    OrderItemDto(
                        productId = it.product.id,
                        productName = it.product.name,
                        quantity = it.quantity,
                        unitPrice = it.product.price,
                        subtotal = it.product.price * it.quantity
                    )
                }
            )
            val remoteResult = orderApiService.createOrder(orderDto)
            Log.d(TAG, "Pedido sincronizado con Web API exitosamente: id=${remoteResult.id}")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar pedido con Web API (modo offline activo): ${e.message}")
        }

        return localId
    }

    override suspend fun syncOrdersFromRemote(): Boolean {
        return try {
            val remoteOrders = orderApiService.getOrders()
            if (remoteOrders.isNotEmpty()) {
                val entities = remoteOrders.map { dto ->
                    OrderEntity(
                        id = dto.id,
                        customerName = dto.customerName,
                        customerPhone = dto.customerPhone,
                        deliveryAddress = dto.deliveryAddress,
                        scheduledTime = dto.scheduledTime,
                        totalAmount = dto.totalAmount,
                        status = dto.status,
                        isExpress = dto.isExpress,
                        itemSummary = dto.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                    )
                }
                orderDao.insertOrders(entities)
                Log.d(TAG, "Sincronización de pedidos exitosa: ${entities.size} pedidos.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al sincronizar pedidos remotos: ${e.message}")
            false
        }
    }

    override suspend fun deleteOrder(id: Long): Boolean {
        val rows = orderDao.deleteOrderById(id)
        if (rows > 0) {
            try {
                orderApiService.cancelOrder(id)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cancelar pedido en la Web API: ${e.message}")
            }
        }
        return rows > 0
    }
}
