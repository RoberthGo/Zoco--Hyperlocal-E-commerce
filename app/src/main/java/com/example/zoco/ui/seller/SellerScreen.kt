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

/** Lo que la pantalla del vendedor puede hacer, agrupado para pasarlo de una pieza a otra. */
private class SellerActions(
    val form: SellerFormActions,
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
        form = SellerFormActions(
            onNameChange, onDescriptionChange, onPriceChange, onStockChange, onCategoryChange,
            onProducerNameChange, onExpressChange, onImageUriChange, onSaveProduct
        ),
        onDeleteOwnProduct = onDeleteOwnProduct,
        onUpdateOrderStatus = onUpdateOrderStatus
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
            SellerFormContent(uiState, actions.form)
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
        SellerFormContent(uiState, actions.form)
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
