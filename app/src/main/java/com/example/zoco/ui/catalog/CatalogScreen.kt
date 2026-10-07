package com.example.zoco.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import kotlinx.coroutines.launch

@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onExpressFilterChange: (Boolean) -> Unit,
    onAddToCart: (Product) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_cart)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.product_category_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryRadioItem(
                label = stringResource(R.string.category_all),
                selected = uiState.selectedCategory == ProductCategory.ALL,
                onClick = { onCategoryChange(ProductCategory.ALL) }
            )
            CategoryRadioItem(
                label = stringResource(R.string.category_bakery),
                selected = uiState.selectedCategory == ProductCategory.BAKERY,
                onClick = { onCategoryChange(ProductCategory.BAKERY) }
            )
            CategoryRadioItem(
                label = stringResource(R.string.category_produce),
                selected = uiState.selectedCategory == ProductCategory.PRODUCE,
                onClick = { onCategoryChange(ProductCategory.PRODUCE) }
            )
            CategoryRadioItem(
                label = stringResource(R.string.category_dairy),
                selected = uiState.selectedCategory == ProductCategory.DAIRY,
                onClick = { onCategoryChange(ProductCategory.DAIRY) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.expressOnly,
                onCheckedChange = onExpressFilterChange
            )
            Text(
                text = stringResource(R.string.filter_express_delivery),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_products_found),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (maxWidth >= 600.dp) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onAddToCart = {
                                onAddToCart(product)
                                scope.launch { snackbarHostState.showSnackbar(addedMessage) }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onAddToCart = {
                                onAddToCart(product)
                                scope.launch { snackbarHostState.showSnackbar(addedMessage) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRadioItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.producerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "\$${"%.2f".format(product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.isExpressDelivery) {
                    Text(
                        text = stringResource(R.string.filter_express_delivery),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(onClick = onAddToCart) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = stringResource(R.string.add_to_cart))
                }
            }
        }
    }
}
