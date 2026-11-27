package com.example.zoco.ui.cart

import com.example.zoco.ui.common.asString
import com.example.zoco.ui.common.formatPrice
import com.example.zoco.ui.common.formatAmount
import com.example.zoco.ui.common.orderStatusLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.widthIn
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.R
import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.model.Product

/** Los botones y campos del carrito agrupados, para pasarlos de una pieza a otra sin repetir una lista larga. */
private class CartActions(
    val onRemoveProduct: (Product) -> Unit,
    val onNameChange: (String) -> Unit,
    val onAddressChange: (String) -> Unit,
    val onExpressChange: (Boolean) -> Unit,
    val onOpenTimePicker: () -> Unit,
    val onConfirmOrder: () -> Unit,
    val onSyncOrders: () -> Unit
)

/**
 * Carrito de compras: productos elegidos, datos de entrega, total y pedidos recientes.
 *  - celular: una sola columna con todo en orden y el total abajo.
 *  - plegable / tablet: dos columnas; productos y pedidos a la izquierda, entrega y total a la derecha.
 */
@Composable
fun CartScreen(
    uiState: CartUiState,
    onRemoveProduct: (Product) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onNameChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onExpressChange: (Boolean) -> Unit = {},
    onConfirmOrder: () -> Unit,
    onSyncOrders: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val actions = CartActions(
        onRemoveProduct = onRemoveProduct,
        onNameChange = onNameChange,
        onAddressChange = onAddressChange,
        onExpressChange = onExpressChange,
        onOpenTimePicker = { showTimePickerDialog = true },
        onConfirmOrder = onConfirmOrder,
        onSyncOrders = onSyncOrders
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // En pantallas muy anchas el contenido no se estira más allá de este ancho
        Box(modifier = Modifier.widthIn(max = CART_MAX_WIDTH).fillMaxSize()) {
            if (LocalWindowSize.current.hasRoomForTwoColumns) {
                CartTwoColumnLayout(uiState, actions)
            } else {
                CartSingleColumnLayout(uiState, actions)
            }
        }
    }

    if (showTimePickerDialog) {
        DeliveryTimePickerDialog(
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { hour, minute ->
                onTimeSelected(hour, minute)
                showTimePickerDialog = false
            }
        )
    }
}

private val CART_MAX_WIDTH = 1100.dp

/** Celular: título, productos, datos de entrega y total, todo en una columna. */
@Composable
private fun CartSingleColumnLayout(uiState: CartUiState, actions: CartActions) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CartTitle(stringResource(R.string.cart_title))
        Spacer(modifier = Modifier.height(10.dp))
        uiState.orderCompletedMessage?.let {
            OrderCompletedBanner(it.asString())
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.items.isEmpty()) {
            EmptyCartMessage(uiState, actions, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.items, key = { it.product.id }) { item ->
                    CartItemRow(item = item, onRemove = { actions.onRemoveProduct(item.product) })
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DeliveryInfoCard(
                        uiState = uiState,
                        onNameChange = actions.onNameChange,
                        onAddressChange = actions.onAddressChange,
                        onExpressChange = actions.onExpressChange,
                        onOpenTimePicker = actions.onOpenTimePicker
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OrderTotalCard(uiState = uiState, onConfirmOrder = actions.onConfirmOrder)
        }
    }
}

/** Mensaje de carrito vacío y, si hay, los pedidos recientes debajo (celular). */
@Composable
private fun EmptyCartMessage(uiState: CartUiState, actions: CartActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.cart_empty), style = MaterialTheme.typography.bodyLarge)
        }
        if (uiState.recentOrders.isNotEmpty()) {
            RecentOrdersSection(uiState.recentOrders, actions.onSyncOrders, modifier = Modifier.weight(1f))
        }
    }
}

/** Plegable o tablet: izquierda productos (o pedidos recientes), derecha entrega y total. */
@Composable
private fun CartTwoColumnLayout(uiState: CartUiState, actions: CartActions) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            CartTitle(stringResource(R.string.cart_title))
            Spacer(modifier = Modifier.height(10.dp))
            uiState.orderCompletedMessage?.let {
                OrderCompletedBanner(it.asString())
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (uiState.items.isEmpty()) {
                Text(text = stringResource(R.string.cart_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items, key = { it.product.id }) { item ->
                        CartItemRow(item = item, onRemove = { actions.onRemoveProduct(item.product) })
                    }
                }
            }
            if (uiState.recentOrders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                RecentOrdersSection(
                    orders = uiState.recentOrders,
                    onSyncOrders = actions.onSyncOrders,
                    modifier = Modifier.weight(if (uiState.items.isEmpty()) 1f else 0.7f)
                )
            }
        }

        if (uiState.items.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(0.9f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CartTitle(stringResource(R.string.cart_summary_title))
                DeliveryInfoCard(
                    uiState = uiState,
                    onNameChange = actions.onNameChange,
                    onAddressChange = actions.onAddressChange,
                    onExpressChange = actions.onExpressChange,
                    onOpenTimePicker = actions.onOpenTimePicker
                )
                OrderTotalCard(uiState = uiState, onConfirmOrder = actions.onConfirmOrder)
            }
        }
    }
}

@Composable
private fun CartTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

/** Lista de pedidos recientes con su botón para actualizarla. */
@Composable
private fun RecentOrdersSection(orders: List<Order>, onSyncOrders: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_orders_title, orders.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSyncOrders, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = stringResource(R.string.sync_orders_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders, key = { it.id }) { order -> RecentOrderCard(order = order) }
        }
    }
}

/** Ventana para elegir la hora de entrega. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryTimePickerDialog(onDismiss: () -> Unit, onTimeSelected: (Int, Int) -> Unit) {
    val timePickerState = rememberTimePickerState(initialHour = 14, initialMinute = 0, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.select_time_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
                        Text(stringResource(R.string.dialog_accept))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCompletedBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DeliveryInfoCard(
    uiState: CartUiState,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onExpressChange: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.delivery_info_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.customerName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.buyer_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = uiState.deliveryAddress,
                onValueChange = onAddressChange,
                label = { Text(stringResource(R.string.delivery_address_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.isExpress,
                        onCheckedChange = onExpressChange
                    )
                    Text(stringResource(R.string.express_delivery_checkbox))
                }

                OutlinedButton(onClick = onOpenTimePicker) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(uiState.scheduledDeliveryTime)
                }
            }
        }
    }
}

@Composable
private fun OrderTotalCard(
    uiState: CartUiState,
    onConfirmOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cart_total_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formatPrice(uiState.totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onConfirmOrder,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.confirm_order_button))
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.quantity} x ${formatPrice(item.product.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = formatPrice(item.product.price * item.quantity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RecentOrderCard(order: Order) {
    val isOnline = order.isRemote || order.customerName.contains("[En Línea]") || order.customerName.contains("Web API")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.order_number_label, order.id),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isOnline) stringResource(R.string.tag_web_api) else stringResource(R.string.tag_room_local),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = orderStatusLabel(order.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (order.customerName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.order_customer_label, order.customerName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (order.itemSummary.isNotBlank()) {
                Text(
                    text = order.itemSummary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.order_time_label, order.scheduledTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = stringResource(R.string.order_total_label, formatAmount(order.totalAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
