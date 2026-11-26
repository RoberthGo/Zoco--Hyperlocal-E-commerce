package com.example.zoco.ui.catalog

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.ui.common.formatPrice

/**
 * Hoja que sube desde abajo para elegir cuántas unidades comprar (celulares y plegables).
 * En tablets grandes se usa [ProductDetailPanel], que muestra lo mismo en un panel fijo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPurchaseBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        PurchaseContent(
            product = product,
            photoHeight = 180.dp,
            onConfirm = onConfirm,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        )
    }
}

/** Panel fijo a la derecha del catálogo en tablets grandes: detalle del producto elegido y botón de compra. */
@Composable
fun ProductDetailPanel(
    product: Product?,
    onConfirm: (Product, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (product == null) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Default.ShoppingBasket,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.pick_product_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    PurchaseContent(
        product = product,
        photoHeight = 220.dp,
        onConfirm = { quantity -> onConfirm(product, quantity) },
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    )
}

/** Foto, datos, selector de cantidad, subtotal y botón de agregar. Lo comparten la hoja y el panel. */
@Composable
private fun PurchaseContent(
    product: Product,
    photoHeight: Dp,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Cada producto empieza en 1 unidad
    var quantity by remember(product.id) { mutableIntStateOf(1) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProductPhoto(product, height = photoHeight)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = product.producerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            StockBadge(product.stock)
        }

        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LabeledAmount(
            label = stringResource(R.string.unit_price_label),
            amount = "${formatPrice(product.price)} MXN",
            amountColor = MaterialTheme.colorScheme.secondary
        )

        QuantitySelector(
            quantity = quantity,
            maxQuantity = product.stock,
            onQuantityChange = { quantity = it }
        )

        LabeledAmount(
            label = stringResource(R.string.subtotal_label),
            amount = "${formatPrice(product.price * quantity)} MXN",
            amountColor = MaterialTheme.colorScheme.primary,
            large = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onConfirm(quantity) },
            enabled = product.stock > 0 && quantity in 1..product.stock,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(text = stringResource(R.string.btn_confirm_add, quantity), fontWeight = FontWeight.Bold)
        }
    }
}

/** Una línea con un texto a la izquierda y una cantidad de dinero a la derecha. */
@Composable
private fun LabeledAmount(label: String, amount: String, amountColor: androidx.compose.ui.graphics.Color, large: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (large) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = amount,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

/** Botones "−" y "+" con la cantidad en medio. No deja bajar de 1 ni pasar de [maxQuantity]. */
@Composable
private fun QuantitySelector(
    quantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.quantity_to_buy),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { onQuantityChange(quantity - 1) },
                enabled = quantity > 1,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.quantity_decrease))
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            FilledTonalButton(
                onClick = { onQuantityChange(quantity + 1) },
                enabled = quantity < maxQuantity,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quantity_increase))
            }
        }
    }
}
