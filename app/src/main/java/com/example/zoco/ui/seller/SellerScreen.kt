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

/** Todos los campos y botones del vendedor agrupados, para pasarlos de una pieza a otra sin repetir una lista larga. */
private class SellerActions(
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onStockChange: (String) -> Unit,
    val onCategoryChange: (ProductCategory) -> Unit,
    val onProducerNameChange: (String) -> Unit,
    val onExpressChange: (Boolean) -> Unit,
    val onImageUriChange: (String?) -> Unit,
    val onSaveProduct: () -> Unit,
    val onDeleteOwnProduct: (Product) -> Unit,
    val onUpdateOrderStatus: (Long, String) -> Unit
)

/**
 * Pantalla del vendedor: publicar productos, ver los propios y atender los pedidos recibidos.
 *  - celular y plegable: tres pestañas (en plegable, el formulario se muestra junto a la vista previa).
 *  - tablet grande: sin pestañas principales; formulario a la izquierda, y a la derecha
 *    "Mis productos" y "Pedidos" en un panel con pestañas.
 */
@Composable
fun SellerScreen(
    uiState: SellerUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit = {},
    onCategoryChange: (ProductCategory) -> Unit,
    onProducerNameChange: (String) -> Unit,
    onExpressChange: (Boolean) -> Unit,
    onImageUriChange: (String?) -> Unit = {},
    onSaveProduct: () -> Unit,
    onDeleteOwnProduct: (Product) -> Unit = {},
    onUpdateOrderStatus: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val actions = SellerActions(
        onNameChange, onDescriptionChange, onPriceChange, onStockChange, onCategoryChange,
        onProducerNameChange, onExpressChange, onImageUriChange, onSaveProduct,
        onDeleteOwnProduct, onUpdateOrderStatus
    )

    Column(modifier = modifier.fillMaxSize()) {
        uiState.actionMessage?.let { ActionMessageBanner(it.asString()) }
        if (LocalWindowSize.current.isExpanded) {
            SellerDashboardLayout(uiState, actions)
        } else {
            SellerTabsLayout(uiState, actions)
        }
    }
}

/** Celular y plegable: una pestaña a la vez. */
@Composable
private fun SellerTabsLayout(uiState: SellerUiState, actions: SellerActions) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.seller_tab_publish),
        stringResource(R.string.seller_tab_my_products, uiState.myProducts.size),
        stringResource(R.string.seller_tab_orders, uiState.receivedOrders.size)
    )
    TabTitles(titles, selectedTab) { selectedTab = it }

    when (selectedTab) {
        0 -> PublishTab(uiState, actions)
        1 -> OwnProductsList(uiState.myProducts, actions.onDeleteOwnProduct)
        else -> ReceivedOrdersList(uiState.receivedOrders, actions.onUpdateOrderStatus)
    }
}

/** Tablet grande: publicar a la izquierda, productos y pedidos a la derecha. */
@Composable
private fun SellerDashboardLayout(uiState: SellerUiState, actions: SellerActions) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SellerFormContent(
                uiState = uiState,
                onNameChange = actions.onNameChange,
                onDescriptionChange = actions.onDescriptionChange,
                onPriceChange = actions.onPriceChange,
                onStockChange = actions.onStockChange,
                onCategoryChange = actions.onCategoryChange,
                onProducerNameChange = actions.onProducerNameChange,
                onExpressChange = actions.onExpressChange,
                onImageUriChange = actions.onImageUriChange,
                onSaveProduct = actions.onSaveProduct
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            TabTitles(
                titles = listOf(
                    stringResource(R.string.seller_tab_my_products, uiState.myProducts.size),
                    stringResource(R.string.seller_tab_orders, uiState.receivedOrders.size)
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )
            if (selectedTab == 0) {
                OwnProductsList(uiState.myProducts, actions.onDeleteOwnProduct, columns = 1)
            } else {
                ReceivedOrdersList(uiState.receivedOrders, actions.onUpdateOrderStatus)
            }
        }
    }
}

/** Fila de pestañas. */
@Composable
private fun TabTitles(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    TabRow(selectedTabIndex = selectedIndex) {
        titles.forEachIndexed { index, title ->
            Tab(selected = selectedIndex == index, onClick = { onSelect(index) }, text = { Text(title) })
        }
    }
}

/** Mensaje corto de lo que acaba de pasar ("Producto publicado", "Pedido actualizado"...). */
@Composable
private fun ActionMessageBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(10.dp)
        )
    }
}

/** Pestaña "Publicar": formulario solo (celular) o formulario junto a la vista previa (plegable). */
@Composable
private fun PublishTab(uiState: SellerUiState, actions: SellerActions) {
    val form = @Composable {
        SellerFormContent(
            uiState = uiState,
            onNameChange = actions.onNameChange,
            onDescriptionChange = actions.onDescriptionChange,
            onPriceChange = actions.onPriceChange,
            onStockChange = actions.onStockChange,
            onCategoryChange = actions.onCategoryChange,
            onProducerNameChange = actions.onProducerNameChange,
            onExpressChange = actions.onExpressChange,
            onImageUriChange = actions.onImageUriChange,
            onSaveProduct = actions.onSaveProduct
        )
    }
    if (LocalWindowSize.current.hasRoomForTwoColumns) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { form() }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { ProductPreview(uiState) }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { form() }
    }
}

/** Cómo se verá el producto en el catálogo mientras se llena el formulario. */
@Composable
private fun ProductPreview(uiState: SellerUiState) {
    Text(
        text = stringResource(R.string.seller_preview_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = stringResource(R.string.seller_preview_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline
    )
    Spacer(modifier = Modifier.height(8.dp))
    val previewProduct = Product(
        id = 0,
        name = uiState.name.ifBlank { stringResource(R.string.placeholder_product_name) },
        description = uiState.description.ifBlank { stringResource(R.string.placeholder_description) },
        price = uiState.price.toDoubleOrNull() ?: 0.0,
        stock = uiState.stock.toIntOrNull() ?: 10,
        category = uiState.category,
        producerName = uiState.producerName.ifBlank { stringResource(R.string.placeholder_producer_name) },
        isExpressDelivery = uiState.isExpressDelivery,
        isRemote = false,
        imageUrl = uiState.imageUri
    )
    ProductItemCard(product = previewProduct, onAddToCart = {})
}

/** Lista de los productos del vendedor. [columns] = null: 1 en celular y 2 si hay espacio. */
@Composable
private fun OwnProductsList(products: List<Product>, onDelete: (Product) -> Unit, columns: Int? = null) {
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
private fun ReceivedOrdersList(orders: List<Order>, onUpdateStatus: (Long, String) -> Unit) {
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

@Composable
private fun SellerFormContent(
    uiState: SellerUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onProducerNameChange: (String) -> Unit,
    onExpressChange: (Boolean) -> Unit,
    onImageUriChange: (String?) -> Unit,
    onSaveProduct: () -> Unit
) {
    Text(
        text = stringResource(R.string.seller_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    if (uiState.isSavedSuccess) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.product_saved_success),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(14.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (uiState.errorMessage != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(14.dp)
            )
        }
    }

    OutlinedTextField(
        value = uiState.name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.product_name_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = uiState.producerName,
        onValueChange = onProducerNameChange,
        label = { Text(stringResource(R.string.product_producer_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = uiState.price,
        onValueChange = onPriceChange,
        label = { Text(stringResource(R.string.product_price_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = uiState.stock,
        onValueChange = onStockChange,
        label = { Text(stringResource(R.string.product_stock_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = uiState.description,
        onValueChange = onDescriptionChange,
        label = { Text(stringResource(R.string.product_description_label)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )

    Text(
        text = stringResource(R.string.product_category_label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    Column {
        listOf(
            ProductCategory.BAKERY to stringResource(R.string.category_bakery),
            ProductCategory.PRODUCE to stringResource(R.string.category_produce),
            ProductCategory.DAIRY to stringResource(R.string.category_dairy),
            ProductCategory.CRAFTS to stringResource(R.string.category_crafts)
        ).forEach { (category, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = uiState.category == category,
                    onClick = { onCategoryChange(category) }
                )
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = uiState.isExpressDelivery,
            onCheckedChange = onExpressChange
        )
        Text(
            text = stringResource(R.string.product_express_checkbox),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    // Selección de Foto / Multimedia (Galería o Cámara)
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageUriChange(uri?.toString())
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "product_photo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            onImageUriChange(Uri.fromFile(file).toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.btn_select_gallery))
        }

        OutlinedButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.btn_take_photo))
        }
    }

    if (!uiState.imageUri.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = uiState.imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = { onImageUriChange(null) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.btn_remove_image),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onSaveProduct,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(R.string.save_product_button))
    }
}

@DeviceScreenSizePreviews
@Composable
fun SellerScreenAdaptivePreview() {
    ZocoTheme {
        Surface {
            SellerScreen(
                uiState = SellerUiState(
                    name = "Miel de Azahar 500g",
                    producerName = "Apícola Los Cedros",
                    price = "120.00",
                    stock = "25",
                    description = "Miel cruda de flores de azahar 100% natural recolectada artesanalmente."
                ),
                onNameChange = {},
                onDescriptionChange = {},
                onPriceChange = {},
                onStockChange = {},
                onCategoryChange = {},
                onProducerNameChange = {},
                onExpressChange = {},
                onImageUriChange = {},
                onSaveProduct = {}
            )
        }
    }
}
