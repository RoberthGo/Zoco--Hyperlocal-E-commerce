package com.example.zoco.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zoco.domain.model.Product
import com.example.zoco.ui.catalog.ProductItemCard

/**
 * Clases de tamaño de ventana (Window Size Classes) según los lineamientos de Material Design 3.
 */
enum class WindowWidthSizeClass {
    COMPACT,   // < 600dp: Teléfonos verticales estándar
    MEDIUM,    // 600dp - 839dp: Plegables, tablets pequeñas o teléfonos apaisados
    EXPANDED   // >= 840dp: Tablets grandes y pantallas de escritorio
}

object AdaptiveBreakpoints {
    val MEDIUM_MIN: Dp = 600.dp
    val EXPANDED_MIN: Dp = 840.dp

    fun getWidthSizeClass(width: Dp): WindowWidthSizeClass = when {
        width < MEDIUM_MIN -> WindowWidthSizeClass.COMPACT
        width < EXPANDED_MIN -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
}

/**
 * Multi-preview annotation: Permite visualizar interfaces en Android Studio
 * en diferentes factores de forma (Teléfono, Paisaje, Plegable y Tablet) simultáneamente.
 */
@Preview(
    name = "1. Teléfono Compacto (Vertical)",
    group = "Dispositivos",
    device = "spec:width=411dp,height=891dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "2. Teléfono Horizontal (Landscape)",
    group = "Dispositivos",
    device = "spec:width=891dp,height=411dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "3. Plegable / Tablet Pequeña (Medio)",
    group = "Dispositivos",
    device = "spec:width=673dp,height=841dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "4. Tablet Grande (Expandido)",
    group = "Dispositivos",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true
)
annotation class DeviceScreenSizePreviews

/**
 * Patrón de diseño adaptable de lista con Jetpack Compose (Adaptive List Pattern):
 * - En COMPACT (<600dp): Despliega un LazyColumn con 1 elemento por fila.
 * - En MEDIUM (600dp..839dp): Despliega un LazyVerticalGrid de 2 columnas fijas.
 * - En EXPANDED (>=840dp): Despliega un LazyVerticalGrid adaptable con tarjetas de mínimo 280dp.
 */
@Composable
fun AdaptiveProductList(
    products: List<Product>,
    widthSizeClass: WindowWidthSizeClass,
    onAddToCart: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
            }
        }
        WindowWidthSizeClass.MEDIUM -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier,
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
            }
        }
        WindowWidthSizeClass.EXPANDED -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = modifier,
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
            }
        }
    }
}
