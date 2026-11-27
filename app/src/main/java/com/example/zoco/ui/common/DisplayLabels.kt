package com.example.zoco.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.zoco.R

/** Texto legible para el usuario de un estado de pedido (los códigos del servidor no se muestran). */
@Composable
fun orderStatusLabel(status: String): String = when (status.trim().uppercase()) {
    "PENDING", "LOCAL_STORED" -> stringResource(R.string.order_status_pending)
    "CONFIRMED" -> stringResource(R.string.order_status_confirmed)
    "PREPARING" -> stringResource(R.string.order_status_preparing)
    "OUT_FOR_DELIVERY" -> stringResource(R.string.order_status_out_for_delivery)
    "DELIVERED" -> stringResource(R.string.order_status_delivered)
    "CANCELLED" -> stringResource(R.string.order_status_cancelled)
    else -> status
}

