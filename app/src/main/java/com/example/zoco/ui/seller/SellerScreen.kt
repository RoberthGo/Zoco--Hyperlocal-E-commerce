package com.example.zoco.ui.seller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zoco.R
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.ui.catalog.ProductItemCard

@Composable
fun SellerScreen(
    uiState: SellerUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onProducerNameChange: (String) -> Unit,
    onExpressChange: (Boolean) -> Unit,
    onImageUriChange: (String?) -> Unit = {},
    onSaveProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Panel izquierdo: Formulario de captura
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SellerFormContent(
                        uiState = uiState,
                        onNameChange = onNameChange,
                        onDescriptionChange = onDescriptionChange,
                        onPriceChange = onPriceChange,
                        onCategoryChange = onCategoryChange,
                        onProducerNameChange = onProducerNameChange,
                        onExpressChange = onExpressChange,
                        onImageUriChange = onImageUriChange,
                        onSaveProduct = onSaveProduct
                    )
                }

                // Panel derecho: Vista previa en tiempo real
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        category = uiState.category,
                        producerName = uiState.producerName.ifBlank { stringResource(R.string.placeholder_producer_name) },
                        isExpressDelivery = uiState.isExpressDelivery,
                        isRemote = false,
                        imageUrl = uiState.imageUri
                    )

                    ProductItemCard(
                        product = previewProduct,
                        onAddToCart = {}
                    )
                }
            }
        } else {
            // Factor de forma móvil estándar (< 600dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SellerFormContent(
                    uiState = uiState,
                    onNameChange = onNameChange,
                    onDescriptionChange = onDescriptionChange,
                    onPriceChange = onPriceChange,
                    onCategoryChange = onCategoryChange,
                    onProducerNameChange = onProducerNameChange,
                    onExpressChange = onExpressChange,
                    onImageUriChange = onImageUriChange,
                    onSaveProduct = onSaveProduct
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

    // Selección de Foto / Multimedia (TASK-FINAL-05)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageUriChange(uri?.toString())
    }

    OutlinedButton(
        onClick = { imagePickerLauncher.launch("image/*") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            if (uiState.imageUri == null) stringResource(R.string.btn_select_image)
            else stringResource(R.string.btn_change_image)
        )
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
