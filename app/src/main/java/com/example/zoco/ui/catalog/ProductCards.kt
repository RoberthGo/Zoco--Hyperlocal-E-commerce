package com.example.zoco.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.ui.common.formatPrice

/**
 * Tarjeta de un producto: foto, nombre, vendedor, precio y botones.
 * Se usa en el catálogo y en la lista de productos del vendedor.
 *  - [onAddToCart]: botón "Agregar".
 *  - [onClick]: tocar la tarjeta (si es null, la tarjeta no reacciona al toque).
 *  - [selected]: dibuja un borde para marcar el producto elegido (tablets).
 */
@Composable
fun ProductItemCard(
    product: Product,
    onAddToCart: () -> Unit,
    onDelete: () -> Unit = {},
    isInternetAvailable: Boolean = true,
    canDelete: Boolean = product.isOwnedByUser,
    isOwnProduct: Boolean = product.isOwnedByUser,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ProductBadges(product)
            ProductPhoto(product, height = 140.dp)
            ProductTitleRow(product)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProductActionsRow(
                product = product,
                onAddToCart = onAddToCart,
                onDelete = onDelete,
                canDelete = canDelete && (isInternetAvailable || !product.isRemote),
                showDelete = canDelete,
                isOwnProduct = isOwnProduct
            )
        }
    }
}

/** Etiquetas de arriba: estado de publicación y unidades disponibles. */
@Composable
private fun ProductBadges(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val published = product.isRemote
        Badge(
            text = when {
                product.pendingSync -> stringResource(R.string.badge_pending_sync)
                published -> stringResource(R.string.badge_online_api)
                else -> stringResource(R.string.badge_local_room)
            },
            background = if (published) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            textColor = if (published) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
        StockBadge(product.stock)
    }
}

/** Etiqueta con las unidades que quedan ("12 disponibles" o "Agotado"). */
@Composable
fun StockBadge(stock: Int, modifier: Modifier = Modifier) {
    val available = stock > 0
    Badge(
        text = if (available) stringResource(R.string.stock_available, stock) else stringResource(R.string.stock_out_of_stock),
        background = if (available) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
        textColor = if (available) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
    )
}

/** Etiqueta pequeña con esquinas redondeadas. */
@Composable
private fun Badge(text: String, background: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(6.dp), color = background, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** Foto del producto con esquinas redondeadas. No dibuja nada si el producto no tiene foto. */
@Composable
fun ProductPhoto(product: Product, height: Dp, modifier: Modifier = Modifier) {
    if (product.imageUrl.isNullOrBlank()) return
    AsyncImage(
        model = product.imageUrl,
        contentDescription = product.name,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(8.dp))
}

/** Nombre y vendedor a la izquierda, precio a la derecha. */
@Composable
private fun ProductTitleRow(product: Product) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = product.producerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = formatPrice(product.price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/** Fila de abajo: envío rápido, etiqueta "Tuyo", botón de borrar y botón de agregar. */
@Composable
private fun ProductActionsRow(
    product: Product,
    onAddToCart: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    showDelete: Boolean,
    isOwnProduct: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product.isExpressDelivery) {
            Text(
                text = stringResource(R.string.filter_express_delivery),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOwnProduct && product.id != 0L) {
                Badge(
                    text = stringResource(R.string.badge_yours),
                    background = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            if (showDelete) {
                IconButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_product_description),
                        tint = if (canDelete) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
            }
            Button(onClick = onAddToCart, enabled = product.stock > 0 && product.inStock) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(text = stringResource(R.string.add_to_cart))
            }
        }
    }
}
