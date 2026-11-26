package com.example.zoco.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.domain.model.ProductCategory

/** Caja de búsqueda y botón para actualizar el catálogo desde el servidor. */
@Composable
fun CatalogSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        FilledTonalButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CloudSync, contentDescription = stringResource(R.string.sync_web_api))
            }
        }
    }
}

/** Fila de "chips" para filtrar por categoría y por envío rápido. Se desliza si no cabe. */
@Composable
fun CategoryFilters(
    selected: ProductCategory,
    onCategoryChange: (ProductCategory) -> Unit,
    expressOnly: Boolean,
    onExpressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categoryChoices().forEach { (category, labelRes) ->
            FilterChip(
                selected = selected == category,
                onClick = { onCategoryChange(category) },
                label = { Text(stringResource(labelRes)) }
            )
        }
        FilterChip(
            selected = expressOnly,
            onClick = { onExpressChange(!expressOnly) },
            label = { Text(stringResource(R.string.filter_express_delivery)) }
        )
    }
}

/** Las categorías que se pueden elegir y el texto de cada una. */
private fun categoryChoices(): List<Pair<ProductCategory, Int>> = listOf(
    ProductCategory.ALL to R.string.category_all,
    ProductCategory.BAKERY to R.string.category_bakery,
    ProductCategory.PRODUCE to R.string.category_produce,
    ProductCategory.DAIRY to R.string.category_dairy,
    ProductCategory.CRAFTS to R.string.category_crafts
)
