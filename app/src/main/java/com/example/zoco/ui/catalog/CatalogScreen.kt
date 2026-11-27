package com.example.zoco.ui.catalog

import com.example.zoco.ui.common.asString
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.ui.common.DeviceScreenSizePreviews
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.ui.common.ProvideWindowSize
import com.example.zoco.ui.common.WidthClass
import com.example.zoco.ui.theme.ZocoTheme
import kotlinx.coroutines.launch

/** Ancho del panel de detalle que aparece a la derecha en tablets grandes. */
private val DETAIL_PANEL_WIDTH = 380.dp

/** Ancho mínimo del contenido (sin menú lateral) para mostrar el panel de detalle. */
private val DETAIL_PANEL_MIN_CONTENT_WIDTH = 800.dp

/**
 * Catálogo de productos. Se ve distinto según el tamaño de la ventana:
 *  - celular: una columna; al tocar "Agregar" sube una hoja para elegir la cantidad.
 *  - plegable / tablet pequeña: cuadrícula de 2 columnas y la misma hoja.
 *  - tablet grande: cuadrícula a la izquierda y panel de detalle fijo a la derecha.
 */
@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onExpressFilterChange: (Boolean) -> Unit,
    onAddToCart: (Product, Int) -> Unit,
    onSyncWeb: () -> Unit = {},
    onDeleteProduct: (Product) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_cart)
    var pickedProduct by remember { mutableStateOf<Product?>(null) }

    // Agrega al carrito y avisa con un mensaje breve
    val addAndNotify: (Product, Int) -> Unit = { product, quantity ->
        onAddToCart(product, quantity)
        scope.launch { snackbarHostState.showSnackbar(addedMessage) }
    }
    // En la tablet el panel siempre muestra la versión más reciente del producto elegido
    val pickedNow = pickedProduct?.let { picked -> uiState.products.firstOrNull { it.id == picked.id } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // El panel de detalle solo cabe si el contenido es ancho; si no, se usa la hoja de abajo
    val showDetailPanel = LocalWindowSize.current.isExpanded && maxWidth >= DETAIL_PANEL_MIN_CONTENT_WIDTH

    val productList = @Composable { listModifier: Modifier ->
        ProductListPane(
            uiState = uiState,
            onSearchChange = onSearchChange,
            onCategoryChange = onCategoryChange,
            onExpressFilterChange = onExpressFilterChange,
            onSyncWeb = onSyncWeb,
            onDeleteProduct = onDeleteProduct,
            onProductPicked = { pickedProduct = it },
            selectedProductId = if (showDetailPanel) pickedNow?.id else null,
            modifier = listModifier
        )
    }

    if (showDetailPanel) {
        Row(modifier = Modifier.fillMaxSize()) {
            productList(Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            ProductDetailPanel(
                product = pickedNow,
                onConfirm = addAndNotify,
                modifier = Modifier
                    .width(DETAIL_PANEL_WIDTH)
                    .fillMaxHeight()
            )
        }
    } else {
        productList(Modifier.fillMaxSize())
        pickedProduct?.let { product ->
            ProductPurchaseBottomSheet(
                product = product,
                onDismiss = { pickedProduct = null },
                onConfirm = { quantity ->
                    addAndNotify(product, quantity)
                    pickedProduct = null
                }
            )
        }
    }
    }
}

/** Buscador, filtros y la lista (o cuadrícula) de productos. */
@Composable
private fun ProductListPane(
    uiState: CatalogUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onExpressFilterChange: (Boolean) -> Unit,
    onSyncWeb: () -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onProductPicked: (Product) -> Unit,
    selectedProductId: Long?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        CatalogSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchChange,
            isRefreshing = uiState.isSyncing,
            onRefresh = onSyncWeb
        )
        uiState.statusMessage?.let { StatusBanner(it.asString()) }
        Spacer(modifier = Modifier.height(8.dp))
        CategoryFilters(
            selected = uiState.selectedCategory,
            onCategoryChange = onCategoryChange,
            expressOnly = uiState.expressOnly,
            onExpressChange = onExpressFilterChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading -> CenteredBox { CircularProgressIndicator() }
            uiState.products.isEmpty() -> CenteredBox { EmptyCatalog(onSyncWeb) }
            else -> ProductGrid(
                uiState = uiState,
                onProductPicked = onProductPicked,
                onDeleteProduct = onDeleteProduct,
                selectedProductId = selectedProductId
            )
        }
    }
}

/** Productos en columnas: 1 en celular, 2 en plegable y tantas como quepan (mínimo 260dp cada una) en tablet. */
@Composable
private fun ProductGrid(
    uiState: CatalogUiState,
    onProductPicked: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    selectedProductId: Long?
) {
    val columns = when (LocalWindowSize.current.widthClass) {
        WidthClass.COMPACT -> GridCells.Fixed(1)
        WidthClass.MEDIUM -> GridCells.Fixed(2)
        WidthClass.EXPANDED -> GridCells.Adaptive(minSize = 260.dp)
    }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.products, key = { it.id }) { product ->
            ProductItemCard(
                product = product,
                onAddToCart = { onProductPicked(product) },
                onDelete = { onDeleteProduct(product) },
                isInternetAvailable = uiState.isInternetAvailable,
                canDelete = uiState.canDelete(product),
                isOwnProduct = uiState.isOwnProduct(product),
                onClick = { onProductPicked(product) },
                selected = product.id == selectedProductId
            )
        }
    }
}

/** Mensaje corto sobre el estado de la conexión o de la última actualización. */
@Composable
private fun StatusBanner(message: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Lo que se ve cuando ningún producto cumple los filtros. */
@Composable
private fun EmptyCatalog(onSyncWeb: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.no_products_found), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSyncWeb) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.sync_web_api))
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@DeviceScreenSizePreviews
@Composable
fun CatalogScreenAdaptivePreview() {
    ZocoTheme {
        Surface {
            ProvideWindowSize {
                CatalogScreen(
                    uiState = CatalogUiState(
                        products = listOf(
                            Product(id = 1, name = "Pan de Masa Madre", description = "Horneado con masa madre tradicional", price = 45.0, category = ProductCategory.BAKERY, stock = 10, producerName = "Panadería Sol"),
                            Product(id = 2, name = "Manzanas Criollas (1kg)", description = "Cosecha fresca del día", price = 32.0, category = ProductCategory.PRODUCE, stock = 8, producerName = "Huerto San José"),
                            Product(id = 3, name = "Queso Fresco Local", description = "Queso artesanal pasteurizado", price = 75.0, category = ProductCategory.DAIRY, stock = 15, producerName = "Lácteos del Sur")
                        )
                    ),
                    onSearchChange = {},
                    onCategoryChange = {},
                    onExpressFilterChange = {},
                    onAddToCart = { _, _ -> },
                    snackbarHostState = remember { SnackbarHostState() }
                )
            }
        }
    }
}
