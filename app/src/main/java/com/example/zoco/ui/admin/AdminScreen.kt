package com.example.zoco.ui.admin

import com.example.zoco.ui.common.formatPrice
import com.example.zoco.ui.common.formatAmount
import com.example.zoco.ui.common.orderStatusLabel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.R
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.UserRole

/**
 * Panel del administrador: usuarios, pedidos y productos de toda la comunidad.
 *  - celular: tres pestañas, una lista a la vez.
 *  - plegable: las mismas pestañas con las tarjetas en 2 columnas.
 *  - tablet grande: tablero con las tres listas lado a lado, sin pestañas.
 */
@Composable
fun AdminScreen(
    uiState: AdminUiState,
    onRefresh: () -> Unit,
    onChangeUserRole: (Long, UserRole) -> Unit,
    onChangeOrderStatus: (Long, String) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val lists = AdminLists(
        userList = { columns -> UserList(uiState.users, columns, onChangeUserRole) },
        orderList = { columns -> OrderList(uiState.orders, columns, onChangeOrderStatus) },
        productList = { columns -> ProductList(uiState.products, columns, onDeleteProduct) }
    )

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        AdminHeader(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh)
        uiState.statusMessage?.let { StatusMessage(it) }
        Spacer(modifier = Modifier.height(8.dp))

        if (LocalWindowSize.current.isExpanded) {
            AdminDashboard(uiState, lists)
        } else {
            AdminTabs(uiState, lists)
        }
    }
}

/** Las tres listas del panel, listas para dibujarse con el número de columnas que se les indique. */
private class AdminLists(
    val userList: @Composable (columns: Int) -> Unit,
    val orderList: @Composable (columns: Int) -> Unit,
    val productList: @Composable (columns: Int) -> Unit
)

/** Título y botón de actualizar. */
@Composable
private fun AdminHeader(isRefreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.admin_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        FilledTonalButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.admin_refresh))
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(10.dp)
        )
    }
}

/** Celular y plegable: una lista a la vez, elegida con pestañas. */
@Composable
private fun AdminTabs(uiState: AdminUiState, lists: AdminLists) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val columns = if (LocalWindowSize.current.hasRoomForTwoColumns) 2 else 1

    TabRow(selectedTabIndex = selectedTab) {
        listOf(
            stringResource(R.string.admin_tab_users, uiState.users.size),
            stringResource(R.string.admin_tab_orders, uiState.orders.size),
            stringResource(R.string.admin_tab_products, uiState.products.size)
        ).forEachIndexed { index, title ->
            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
        }
    }
    Spacer(modifier = Modifier.height(10.dp))

    when (selectedTab) {
        0 -> lists.userList(columns)
        1 -> lists.orderList(columns)
        else -> lists.productList(columns)
    }
}

/** Tablet grande: usuarios, pedidos y productos lado a lado, cada uno con su título. */
@Composable
private fun AdminDashboard(uiState: AdminUiState, lists: AdminLists) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DashboardColumn(stringResource(R.string.admin_tab_users, uiState.users.size), Modifier.weight(1f)) { lists.userList(1) }
        DashboardColumn(stringResource(R.string.admin_tab_orders, uiState.orders.size), Modifier.weight(1f)) { lists.orderList(1) }
        DashboardColumn(stringResource(R.string.admin_tab_products, uiState.products.size), Modifier.weight(1f)) { lists.productList(1) }
    }
}

@Composable
private fun DashboardColumn(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun UserList(users: List<ManagedUser>, columns: Int, onChangeRole: (Long, UserRole) -> Unit) {
    CardGrid(users, key = { it.username }, columns = columns) { AdminUserCard(user = it, onChangeRole = onChangeRole) }
}

@Composable
private fun OrderList(orders: List<Order>, columns: Int, onChangeStatus: (Long, String) -> Unit) {
    CardGrid(orders, key = { it.id }, columns = columns) { AdminOrderCard(order = it, onChangeStatus = onChangeStatus) }
}

@Composable
private fun ProductList(products: List<Product>, columns: Int, onDelete: (Product) -> Unit) {
    CardGrid(products, key = { it.id }, columns = columns) { AdminProductCard(product = it, onDelete = { onDelete(it) }) }
}

/** Tarjetas en [columns] columnas con desplazamiento. Sirve para cualquier tipo de elemento. */
@Composable
private fun <T> CardGrid(items: List<T>, key: (T) -> Any, columns: Int, card: @Composable (T) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = key) { item -> card(item) }
    }
}

@Composable
private fun AdminUserCard(
    user: ManagedUser,
    onChangeRole: (Long, UserRole) -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user.username} • ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = when (user.role) {
                            UserRole.BUYER -> stringResource(R.string.role_buyer)
                            UserRole.SELLER -> stringResource(R.string.role_seller)
                            UserRole.ADMIN -> stringResource(R.string.role_admin)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    UserRole.BUYER to stringResource(R.string.role_buyer),
                    UserRole.SELLER to stringResource(R.string.role_seller),
                    UserRole.ADMIN to stringResource(R.string.role_admin)
                ).forEach { (role, label) ->
                    FilterChip(
                        selected = user.role == role,
                        onClick = { onChangeRole(user.id, role) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: Order,
    onChangeStatus: (Long, String) -> Unit
) {
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
                Text(
                    text = stringResource(R.string.order_number_label, order.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = orderStatusLabel(order.status),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (order.customerName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.order_customer_label, order.customerName),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (order.itemSummary.isNotBlank()) {
                Text(
                    text = order.itemSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = stringResource(R.string.order_total_label, formatAmount(order.totalAmount)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "CONFIRMED" to stringResource(R.string.btn_mark_confirmed),
                    "PREPARING" to stringResource(R.string.btn_mark_preparing),
                    "OUT_FOR_DELIVERY" to stringResource(R.string.btn_mark_delivery),
                    "DELIVERED" to stringResource(R.string.btn_mark_delivered)
                ).forEach { (statusCode, label) ->
                    OutlinedButton(
                        onClick = { onChangeStatus(order.id, statusCode) },
                        enabled = order.status != statusCode,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminProductCard(
    product: Product,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.admin_owner_label,
                        product.sellerUsername ?: product.producerName
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${formatPrice(product.price)} • ${stringResource(R.string.stock_available, product.stock)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_product_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
