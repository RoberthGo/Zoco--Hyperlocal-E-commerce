package com.example.zoco.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.zoco.R
import com.example.zoco.domain.model.UiMessage

/** Convierte un [UiMessage] en el texto final, en el idioma que esté elegido en la app. */
@Composable
fun UiMessage.asString(): String {
    val id = resId ?: return rawText.orEmpty()
    val resolvedArgs = args.map { if (it is UiMessage) it.asString() else it }
    return stringResource(id, *resolvedArgs.toTypedArray())
}

/** Mensaje con el nombre del estado de un pedido ("Pendiente", "En camino"...). */
fun orderStatusMessage(status: String): UiMessage = when (status.trim().uppercase()) {
    "PENDING", "LOCAL_STORED" -> UiMessage.of(R.string.order_status_pending)
    "CONFIRMED" -> UiMessage.of(R.string.order_status_confirmed)
    "PREPARING" -> UiMessage.of(R.string.order_status_preparing)
    "OUT_FOR_DELIVERY" -> UiMessage.of(R.string.order_status_out_for_delivery)
    "DELIVERED" -> UiMessage.of(R.string.order_status_delivered)
    "CANCELLED" -> UiMessage.of(R.string.order_status_cancelled)
    else -> UiMessage.raw(status)
}

/** Mensaje con el nombre de un rol ("Comprador", "Vendedor", "Administrador"). */
fun roleMessage(role: com.example.zoco.domain.model.UserRole): UiMessage = UiMessage.of(
    when (role) {
        com.example.zoco.domain.model.UserRole.BUYER -> R.string.role_buyer
        com.example.zoco.domain.model.UserRole.SELLER -> R.string.role_seller
        com.example.zoco.domain.model.UserRole.ADMIN -> R.string.role_admin
    }
)
