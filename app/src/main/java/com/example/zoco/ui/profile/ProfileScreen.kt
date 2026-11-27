package com.example.zoco.ui.profile

import com.example.zoco.ui.common.asString
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.widthIn
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.R
import com.example.zoco.domain.model.UserRole
import java.io.File

/**
 * Pestaña "Perfil". Es una columna con tarjetas, de arriba hacia abajo:
 *  1. foto, nombre y rol            ([ProfileHeaderCard])
 *  2. convertirse en vendedor       ([BecomeSellerCard], solo compradores)
 *  3. cerrar sesión                 ([LogoutCard])
 *  4. idioma                        ([LanguageCard])
 *  5. integrantes del equipo        ([TeamCard])
 * La pantalla no guarda datos: todo lo que el usuario hace se avisa con las funciones `on...`
 * y lo resuelve [ProfileViewModel].
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState = ProfileUiState(),
    onUpdatePhoto: (String?) -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onBecomeSeller: (String?) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePassword: (current: String, new: String) -> Unit = { _, _ -> },
    onPasswordDialogClosed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Tarjetas de la cuenta (foto, vendedor, cerrar sesión) y tarjetas de ajustes (idioma, equipo)
    val accountCards = @Composable {
        ProfileHeaderCard(uiState, onUpdatePhoto, onUpdateName)
        if (uiState.currentSession.role == UserRole.BUYER) {
            BecomeSellerCard(onBecomeSeller)
        }
        ChangePasswordCard(
            isBusy = uiState.isAuthLoading,
            passwordChanged = uiState.passwordChanged,
            onChangePassword = onChangePassword,
            onDialogClosed = onPasswordDialogClosed
        )
        LogoutCard(isBusy = uiState.isAuthLoading, onLogout = onLogout)
    }
    val settingsCards = @Composable {
        LanguageCard(selectedLanguage = uiState.selectedLanguage, onLanguageChange = onLanguageChange)
        TeamCard()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = PROFILE_MAX_WIDTH)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (LocalWindowSize.current.hasRoomForTwoColumns) {
                // Tablet o plegable: cuenta a la izquierda, ajustes a la derecha
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { accountCards() }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { settingsCards() }
                }
            } else {
                accountCards()
                settingsCards()
            }
        }
    }
}

/** Ancho máximo del contenido del perfil en pantallas muy anchas. */
private val PROFILE_MAX_WIDTH = 960.dp

// ---------------------------------------------------------------------------------------------
// 1. Foto, nombre y rol
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfileHeaderCard(
    uiState: ProfileUiState,
    onUpdatePhoto: (String?) -> Unit,
    onUpdateName: (String) -> Unit
) {
    val photoPicker = rememberPhotoPicker(onPhotoChosen = onUpdatePhoto)
    var showNameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(photoUri = uiState.photoUri, description = uiState.userName)
                NameAndRole(
                    name = uiState.userName,
                    username = uiState.currentSession.username,
                    role = uiState.currentSession.role,
                    onEditName = { showNameDialog = true },
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
            }

            // Mensaje de la última acción (por ejemplo, "no se pudo subir la foto")
            uiState.authMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            PhotoButtons(photoPicker)
        }
    }

    if (showNameDialog) {
        EditNameDialog(
            currentName = uiState.userName,
            onSave = onUpdateName,
            onDismiss = { showNameDialog = false }
        )
    }
}

/** Nombre con un lápiz para editarlo, y debajo la etiqueta del rol y el nombre de usuario. */
@Composable
private fun NameAndRole(
    name: String,
    username: String,
    role: UserRole,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.btn_edit_profile),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoleBadge(role = role)
            Text(text = "@$username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Botones "Galería" y "Tomar foto". */
@Composable
private fun PhotoButtons(photoPicker: PhotoPicker) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = photoPicker.pickFromGallery, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.btn_select_gallery), style = MaterialTheme.typography.labelMedium)
        }
        FilledTonalButton(onClick = photoPicker.takePhoto, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.btn_take_photo), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Foto circular del usuario; si no tiene, muestra un ícono de persona. */
@Composable
private fun ProfilePhoto(photoUri: String?, description: String) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Etiqueta de color con el rol (Comprador, Vendedor o Administrador). */
@Composable
private fun RoleBadge(role: UserRole) {
    val backgroundColor = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.errorContainer
        UserRole.SELLER -> MaterialTheme.colorScheme.tertiaryContainer
        UserRole.BUYER -> MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.onErrorContainer
        UserRole.SELLER -> MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.BUYER -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val label = when (role) {
        UserRole.BUYER -> stringResource(R.string.role_buyer)
        UserRole.SELLER -> stringResource(R.string.role_seller)
        UserRole.ADMIN -> stringResource(R.string.role_admin)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = backgroundColor) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_edit_name)) },
        text = {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editedName.isNotBlank()) onSave(editedName.trim())
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Elegir o tomar una foto
// ---------------------------------------------------------------------------------------------

/** Las dos acciones para cambiar la foto: elegir de la galería o tomarla con la cámara. */
private class PhotoPicker(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit
)

/**
 * Prepara la galería y la cámara. Cuando el usuario termina, se llama a [onPhotoChosen]
 * con la dirección (URI) de la foto. Para la cámara primero se pide el permiso si hace falta.
 */
@Composable
private fun rememberPhotoPicker(onPhotoChosen: (String?) -> Unit): PhotoPicker {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onPhotoChosen(uri.toString())
    }

    // La cámara devuelve una miniatura en memoria: se guarda en un archivo temporal para tener su URI.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            onPhotoChosen(Uri.fromFile(file).toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
    }

    return PhotoPicker(
        pickFromGallery = { galleryLauncher.launch("image/*") },
        takePhoto = {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    )
}
