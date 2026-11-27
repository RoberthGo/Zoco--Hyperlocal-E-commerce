package com.example.zoco.ui.seller

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.zoco.R
import com.example.zoco.domain.model.ProductCategory
import java.io.File

/** Los campos y botones del formulario "Publicar producto", agrupados para pasarlos juntos. */
internal class SellerFormActions(
    val onNameChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onStockChange: (String) -> Unit,
    val onCategoryChange: (ProductCategory) -> Unit,
    val onProducerNameChange: (String) -> Unit,
    val onExpressChange: (Boolean) -> Unit,
    val onImageUriChange: (String?) -> Unit,
    val onSaveProduct: () -> Unit
)

/**
 * Formulario para publicar un producto, de arriba hacia abajo:
 * título, avisos, datos escritos, categoría, envío rápido, foto y botón de guardar.
 * Dibuja sus piezas una debajo de otra; quien lo llama decide en qué columna las coloca.
 */
@Composable
internal fun SellerFormContent(uiState: SellerUiState, actions: SellerFormActions) {
    Text(
        text = stringResource(R.string.seller_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    FormBanners(uiState)
    ProductTextFields(uiState, actions)
    CategoryPicker(selected = uiState.category, onCategoryChange = actions.onCategoryChange)
    ExpressDeliveryCheckbox(checked = uiState.isExpressDelivery, onCheckedChange = actions.onExpressChange)
    ProductPhotoPicker(imageUri = uiState.imageUri, onImageUriChange = actions.onImageUriChange)
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = actions.onSaveProduct, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.save_product_button))
    }
}

/** Aviso verde si se guardó el producto y aviso rojo si el formulario tiene un error. */
@Composable
private fun FormBanners(uiState: SellerUiState) {
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
    uiState.errorMessage?.let { error ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
        }
    }
}

/** Nombre, vendedor, precio, unidades y descripción. */
@Composable
private fun ProductTextFields(uiState: SellerUiState, actions: SellerFormActions) {
    FormTextField(uiState.name, actions.onNameChange, R.string.product_name_label)
    FormTextField(uiState.producerName, actions.onProducerNameChange, R.string.product_producer_label)
    FormTextField(uiState.price, actions.onPriceChange, R.string.product_price_label, KeyboardType.Decimal)
    FormTextField(uiState.stock, actions.onStockChange, R.string.product_stock_label, KeyboardType.Number)
    OutlinedTextField(
        value = uiState.description,
        onValueChange = actions.onDescriptionChange,
        label = { Text(stringResource(R.string.product_description_label)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}

/** Campo de texto de una línea. */
@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/** Lista de categorías con un botón de selección cada una. */
@Composable
private fun CategoryPicker(selected: ProductCategory, onCategoryChange: (ProductCategory) -> Unit) {
    Text(
        text = stringResource(R.string.product_category_label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    Column {
        listOf(
            ProductCategory.BAKERY to R.string.category_bakery,
            ProductCategory.PRODUCE to R.string.category_produce,
            ProductCategory.DAIRY to R.string.category_dairy,
            ProductCategory.CRAFTS to R.string.category_crafts
        ).forEach { (category, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = selected == category, onClick = { onCategoryChange(category) })
                Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExpressDeliveryCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = stringResource(R.string.product_express_checkbox), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Botones para elegir la foto (galería o cámara) y vista previa con una "X" para quitarla.
 * La cámara pide permiso la primera vez.
 */
@Composable
private fun ProductPhotoPicker(imageUri: String?, onImageUriChange: (String?) -> Unit) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onImageUriChange(uri?.toString())
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "product_photo_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            onImageUriChange(Uri.fromFile(file).toString())
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.btn_select_gallery))
        }
        OutlinedButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (hasPermission) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.btn_take_photo))
        }
    }

    if (!imageUri.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            IconButton(onClick = { onImageUriChange(null) }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.btn_remove_image),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
