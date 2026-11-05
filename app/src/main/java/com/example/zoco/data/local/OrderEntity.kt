package com.example.zoco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zoco.domain.model.Order

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val scheduledTime: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "PENDING",
    val isExpress: Boolean = false,
    val itemSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRemote: Boolean = false
) {
    fun toDomain(): Order {
        return Order(
            id = id,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            scheduledTime = scheduledTime,
            totalAmount = totalAmount,
            status = status,
            isExpress = isExpress,
            itemSummary = itemSummary,
            createdAt = createdAt,
            isRemote = isRemote
        )
    }

    companion object {
        fun fromDomain(order: Order): OrderEntity {
            return OrderEntity(
                id = order.id,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                deliveryAddress = order.deliveryAddress,
                scheduledTime = order.scheduledTime,
                totalAmount = order.totalAmount,
                status = order.status,
                isExpress = order.isExpress,
                itemSummary = order.itemSummary,
                createdAt = order.createdAt,
                isRemote = order.isRemote
            )
        }
    }
}
