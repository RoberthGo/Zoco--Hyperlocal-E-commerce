package com.example.zoco.ui.seller

import com.example.zoco.ui.common.asString
import com.example.zoco.ui.common.formatPrice
import com.example.zoco.ui.common.orderStatusLabel
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.material3.Surface
import com.example.zoco.domain.model.Order
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.ui.catalog.ProductItemCard
import com.example.zoco.ui.common.DeviceScreenSizePreviews
import com.example.zoco.ui.theme.ZocoTheme
import java.io.File

// Listas de la pantalla del vendedor: sus productos y los pedidos que recibe.

/** Lista de los productos del vendedor. [columns] = null: 1 en celular y 2 si hay espacio. */
@Composable
internal fun OwnProductsList(products: List<Product>, onDelete: (Product) -> Unit, columns: Int? = null) {
    if (products.isEmpty()) {
        EmptyListMessage(stringResource(R.string.seller_no_products))
        return
    }
    val columnCount = columns ?: if (LocalWindowSize.current.hasRoomForTwoColumns) 2 else 1
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(products, key = { it.id }) { product ->
            SellerOwnProductCard(product = product, onDelete = { onDelete(product) })
        }
    }
}

/** Lista de pedidos que incluyen productos del vendedor, con botones para cambiar su estado. */
@Composable
internal fun ReceivedOrdersList(orders: List<Order>, onUpdateStatus: (Long, String) -> Unit) {
    if (orders.isEmpty()) {
        EmptyListMessage(stringResource(R.string.seller_no_orders))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(orders, key = { it.id }) { order -> ReceivedOrderCard(order, onUpdateStatus) }
    }
}

@Composable
private fun EmptyListMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Un pedido recibido: número, estado, cliente, productos y botones para avanzar el estado. */
@Composable
private fun ReceivedOrderCard(order: Order, onUpdateStatus: (Long, String) -> Unit) {
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
                Text(text = order.itemSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "CONFIRMED" to stringResource(R.string.btn_mark_confirmed),
                    "PREPARING" to stringResource(R.string.btn_mark_preparing),
                    "OUT_FOR_DELIVERY" to stringResource(R.string.btn_mark_delivery),
                    "DELIVERED" to stringResource(R.string.btn_mark_delivered)
                ).forEach { (statusCode, label) ->
                    OutlinedButton(
                        onClick = { onUpdateStatus(order.id, statusCode) },
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
private fun SellerOwnProductCard(
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
                    text = "${product.producerName} • ${formatPrice(product.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.stock_available, product.stock),
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
