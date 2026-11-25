package com.example.zoco.ui.profile

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.zoco.R
import com.example.zoco.data.remote.RetrofitClient
import java.io.File

@Composable
fun ProfileScreen(
    uiState: ProfileUiState = ProfileUiState(),
    onUpdatePhoto: (String?) -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onApiUrlChange: (String) -> Unit = {},
    onCheckConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chipScrollState = rememberScrollState()
    var showNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(uiState.userName) }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdatePhoto(uri.toString())
        }
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            onUpdatePhoto(Uri.fromFile(file).toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    Column(
        modifier = modifier
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

        // Tarjeta de Usuario / Vendedor con Foto Editable
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = uiState.photoUri,
                                contentDescription = uiState.userName,
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    editedName = uiState.userName
                                    showNameDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.btn_edit_profile),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = uiState.userRole,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.btn_select_gallery), style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.btn_take_photo), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
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
                            if (editedName.isNotBlank()) {
                                onUpdateName(editedName.trim())
                            }
                            showNameDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.dialog_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }

        // Configuración y Estado de Conexión a Web API (Retrofit)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.api_connection_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.api_url_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.apiUrl,
                    onValueChange = onApiUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Accesos directos rápidos para cambiar de IP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onApiUrlChange(RetrofitClient.CURRENT_WIFI_URL) },
                        label = { Text(stringResource(R.string.chip_wifi_pc)) }
                    )
                    AssistChip(
                        onClick = { onApiUrlChange(RetrofitClient.LOCALHOST_URL) },
                        label = { Text(stringResource(R.string.chip_localhost)) }
                    )
                    AssistChip(
                        onClick = { onApiUrlChange(RetrofitClient.EMULATOR_LOOPBACK_URL) },
                        label = { Text(stringResource(R.string.chip_emulator)) }
                    )
                    AssistChip(
                        onClick = { onApiUrlChange(RetrofitClient.PREVIOUS_WIFI_URL) },
                        label = { Text(stringResource(R.string.chip_wifi_home)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCheckConnection,
                        enabled = !uiState.isCheckingConnection
                    ) {
                        if (uiState.isCheckingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Text(stringResource(R.string.btn_test_connection))
                    }

                    if (uiState.isOnline != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.isOnline == true) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (uiState.isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (uiState.isOnline == true) stringResource(R.string.status_online) else stringResource(R.string.status_offline),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (uiState.connectionStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.connectionStatus ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Preferencias de Idioma
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.preferences_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.selectedLanguage == "es",
                        onClick = { onLanguageChange("es") }
                    )
                    Text(stringResource(R.string.language_es), modifier = Modifier.padding(end = 16.dp))

                    RadioButton(
                        selected = uiState.selectedLanguage == "en",
                        onClick = { onLanguageChange("en") }
                    )
                    Text(stringResource(R.string.language_en))
                }
            }
        }

        // Integrantes del Equipo (Criterio para Evaluación Presencial)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.team_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.team_members),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
