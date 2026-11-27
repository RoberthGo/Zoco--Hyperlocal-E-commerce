package com.example.zoco.domain.repository

import com.example.zoco.domain.model.NotificationItem

/** Avisos de la cuenta (por ejemplo, cambios de estado de un pedido). Siempre se leen del servidor. */
interface NotificationRepository {
    /** Descarga las notificaciones de la persona con sesión iniciada, las más nuevas primero. */
    suspend fun fetch(): Result<List<NotificationItem>>

    suspend fun markRead(id: Long): Boolean

    suspend fun markAllRead(): Boolean
}
