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
import com.example.zoco.domain.sync.SyncScheduler
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Implementación de [OrderRepository]. Primero guarda en la base local (Room) y luego intenta subir al
 * servidor; si no hay red, el pedido queda marcado como pendiente y se reintenta después.
 * Si el servidor no responde al descargar, borra de la base local los pedidos que vinieron de él
 * para no mostrar datos viejos.
 */
class OrderRepositoryImpl(
    private val orderDao: OrderDao,
    private val customApiService: OrderApiService? = null,
    private val syncScheduler: SyncScheduler? = null
) : OrderRepository {

    private val orderApiService: OrderApiService
        get() = customApiService ?: RetrofitClient.orderApiService

    companion object {
        private const val TAG = "OrderRepository"

        // Serializa/deserializa los renglones del pedido guardados en OrderEntity.itemsJson
        private val itemsAdapter: JsonAdapter<List<OrderItemDto>> by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(Types.newParameterizedType(List::class.java, OrderItemDto::class.java))
        }
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

        // 2. Guardar en Room como PENDIENTE de subir, junto con los renglones (para poder reenviarlo)
        val itemDtos = items.map {
            OrderItemDto(
                productId = it.product.id,
                productName = it.product.name,
                quantity = it.quantity,
                unitPrice = it.product.price,
                subtotal = it.product.price * it.quantity
            )
        }
        val localId = orderDao.insertOrder(
            OrderEntity.fromDomain(localOrder).copy(
                isRemote = false,
                pendingSync = true,
                itemsJson = itemsAdapter.toJson(itemDtos)
            )
        )

        // 3. Sincronización Web API (Retrofit POST /api/v1/orders); si falla, WorkManager lo reintenta
        val entity = orderDao.getOrderById(localId)
        if (entity != null) {
            try {
                uploadOrder(entity)
            } catch (e: Exception) {
                Log.w(TAG, "Sin conexión con la Web API; pedido id=$localId queda pendiente: ${e.message}")
                syncScheduler?.scheduleSync()
            }
        }

        return localId
    }

    /**
     * Sube UN pedido pendiente (POST). Lanza excepción si falla. Al tener éxito reemplaza la fila
     * local por una con el id del servidor (isRemote = true, pendingSync = false).
     */
    private suspend fun uploadOrder(entity: OrderEntity) {
        val items = if (entity.itemsJson.isBlank()) emptyList()
        else itemsAdapter.fromJson(entity.itemsJson).orEmpty()

        val orderDto = OrderDto(
            customerName = entity.customerName.ifBlank { "Comprador Zoco" },
            customerPhone = entity.customerPhone,
            deliveryAddress = entity.deliveryAddress,
            scheduledTime = entity.scheduledTime,
            totalAmount = entity.totalAmount,
            status = entity.status,
            isExpress = entity.isExpress,
            items = items
        )
        val created = orderApiService.createOrder(orderDto)
        orderDao.deleteOrderById(entity.id)
        orderDao.insertOrder(
            entity.copy(
                id = created.id,
                status = created.status,
                isRemote = true,
                pendingSync = false,
                itemsJson = ""
            )
        )
        Log.d(TAG, "Pedido subido a la Web API: id local=${entity.id} -> id servidor=${created.id}")
    }

    /**
     * Vacía la cola de pedidos pendientes. Se detiene al primer error de red.
     * Devuelve true solo si no quedó ningún pedido pendiente.
     */
    override suspend fun uploadPending(): Boolean {
        var allPushed = true
        for (entity in orderDao.getPendingOrders().sortedBy { it.id }) {
            try {
                uploadOrder(entity)
            } catch (e: IOException) {
                Log.w(TAG, "Sin red al subir pedidos pendientes: ${e.message}")
                return false
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo subir pedido id=${entity.id}: ${e.message}")
                allPushed = false
            }
        }
        return allPushed
    }

    override suspend fun refreshOrders(): Boolean {
        return try {
            val remoteOrders = orderApiService.getOrders()
            if (remoteOrders.isNotEmpty()) {
                orderDao.deleteRemoteOrders()
                val pendingIds = orderDao.getPendingOrders().map { it.id }.toSet()
                val entities = remoteOrders.filter { it.id !in pendingIds }.map { dto ->
                    OrderEntity(
                        id = dto.id,
                        customerName = dto.customerName,
                        customerPhone = dto.customerPhone,
                        deliveryAddress = dto.deliveryAddress,
                        scheduledTime = dto.scheduledTime,
                        totalAmount = dto.totalAmount,
                        status = dto.status,
                        isExpress = dto.isExpress,
                        itemSummary = dto.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                        isRemote = true
                    )
                }
                orderDao.insertOrders(entities)
                Log.d(TAG, "Sincronización de pedidos exitosa: ${entities.size} pedidos.")
            }
            // El servidor respondió: aprovechar la conexión para subir pedidos hechos offline
            uploadPending()
            remoteOrders.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Web API no activa (${e.message}). Eliminando pedidos remotos de Room para mostrar únicamente pedidos locales.")
            orderDao.deleteRemoteOrders()
            false
        }
    }

    override suspend fun clearLocalOrders() {
        orderDao.clearAll()
    }

    override suspend fun removeSampleOrders() {
        orderDao.deleteOldSampleOrders()
    }

    override suspend fun updateOrderStatus(orderId: Long, newStatus: String): Boolean {
        val entity = orderDao.getOrderById(orderId) ?: return false
        if (entity.isRemote) {
            try {
                orderApiService.updateOrderStatus(orderId, mapOf("status" to newStatus))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar estado en Web API; guardando cambio local: ${e.message}")
            }
        }
        return orderDao.updateOrder(entity.copy(status = newStatus)) > 0
    }

    override suspend fun deleteOrder(id: Long): Boolean {
        val entity = orderDao.getOrderById(id) ?: return false

        // Regla: cancelar/eliminar solo con internet cuando el pedido existe en el servidor
        if (entity.isRemote) {
            try {
                val response = orderApiService.cancelOrder(id)
                if (!response.isSuccessful && response.code() != 404) {
                    Log.w(TAG, "La Web API rechazó cancelar el pedido id=$id (HTTP ${response.code()})")
                    return false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sin conexión: no se puede cancelar pedido id=$id: ${e.message}")
                return false
            }
        }
        return orderDao.deleteOrderById(id) > 0
    }
}
