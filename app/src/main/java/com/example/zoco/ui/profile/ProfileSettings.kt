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

// Tarjetas de ajustes del perfil: contraseña, cerrar sesión, idioma y equipo.

// ---------------------------------------------------------------------------------------------
// 3a. Cambiar contraseña
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ChangePasswordCard(
    isBusy: Boolean,
    passwordChanged: Boolean,
    onChangePassword: (String, String) -> Unit,
    onDialogClosed: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    // Cuando el servidor acepta el cambio, la ventana se cierra sola
    if (passwordChanged && showDialog) {
        showDialog = false
        onDialogClosed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            OutlinedButton(onClick = { showDialog = true }, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_change_password))
            }
        }
    }

    if (showDialog) {
        ChangePasswordDialog(
            isBusy = isBusy,
            onConfirm = onChangePassword,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    isBusy: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val tooShort = new.isNotEmpty() && new.length < 6
    val mismatch = repeat.isNotEmpty() && repeat != new
    val canSave = current.isNotBlank() && new.length >= 6 && new == repeat && !isBusy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordInput(current, { current = it }, stringResource(R.string.password_current))
                PasswordInput(new, { new = it }, stringResource(R.string.password_new), isError = tooShort)
                PasswordInput(repeat, { repeat = it }, stringResource(R.string.auth_confirm_password_label), isError = mismatch)
                if (tooShort) {
                    Text(stringResource(R.string.auth_error_password_short), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (mismatch) {
                    Text(stringResource(R.string.auth_error_password_mismatch), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current, new) }, enabled = canSave) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun PasswordInput(value: String, onValueChange: (String) -> Unit, label: String, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------------------------
// 3. Cerrar sesión
// ---------------------------------------------------------------------------------------------

@Composable
internal fun LogoutCard(isBusy: Boolean, onLogout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.logout_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onLogout,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.btn_logout))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 4. Idioma
// ---------------------------------------------------------------------------------------------

@Composable
internal fun LanguageCard(selectedLanguage: String, onLanguageChange: (String) -> Unit) {
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
                    selected = selectedLanguage == "es",
                    onClick = { onLanguageChange("es") }
                )
                Text(stringResource(R.string.language_es), modifier = Modifier.padding(end = 16.dp))

                RadioButton(
                    selected = selectedLanguage == "en",
                    onClick = { onLanguageChange("en") }
                )
                Text(stringResource(R.string.language_en))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 5. Integrantes del equipo (criterio de la evaluación presencial)
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TeamCard() {
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
