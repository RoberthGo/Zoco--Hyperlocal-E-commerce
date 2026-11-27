package com.example.zoco.ui.auth

import com.example.zoco.ui.common.asString
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.R

/**
 * Pantalla de acceso obligatoria: pestañas Iniciar sesión / Registrarse.
 * El registro solo crea cuentas de comprador; los administradores únicamente inician sesión.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (username: String, email: String, password: String, fullName: String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val errorRequired = stringResource(R.string.auth_error_required)
    val errorShort = stringResource(R.string.auth_error_password_short)
    val errorMismatch = stringResource(R.string.auth_error_password_mismatch)
    val errorEmail = stringResource(R.string.auth_error_email_invalid)

    val isRegister = tabIndex == 1
    val shownError = localError ?: uiState.errorMessage?.asString()

    fun submit() {
        localError = null
        if (!isRegister) {
            if (username.isBlank() || password.isBlank()) {
                localError = errorRequired
                return
            }
            onLogin(username, password)
        } else {
            localError = when {
                username.isBlank() || fullName.isBlank() || email.isBlank() || password.isBlank() -> errorRequired
                !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> errorEmail
                password.length < 6 -> errorShort
                password != confirmPassword -> errorMismatch
                else -> null
            }
            if (localError == null) onRegister(username, email, password, fullName)
        }
    }

    val windowSize = LocalWindowSize.current
    val useSplitLayout = windowSize.hasRoomForTwoColumns

    // Campos y botón; se dibujan igual en todos los tamaños, solo cambia dónde se colocan
    val form = @Composable {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = !isRegister,
                onClick = {
                    tabIndex = 0
                    localError = null
                    onClearError()
                },
                text = { Text(stringResource(R.string.auth_dialog_login_tab)) }
            )
            Tab(
                selected = isRegister,
                onClick = {
                    tabIndex = 1
                    localError = null
                    onClearError()
                },
                text = { Text(stringResource(R.string.auth_dialog_register_tab)) }
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.auth_username_label)) },
            singleLine = true,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        if (isRegister) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.auth_fullname_label)) },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.auth_password_label),
            enabled = !uiState.isLoading
        )
        if (isRegister) {
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(R.string.auth_confirm_password_label),
                enabled = !uiState.isLoading
            )
            Text(
                text = stringResource(R.string.auth_register_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (shownError != null) {
            Text(
                text = shownError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = ::submit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (isRegister) stringResource(R.string.auth_submit_register)
                    else stringResource(R.string.auth_submit_login)
                )
            }
        }
    }

    if (useSplitLayout) {
        // Tablet o plegable: panel con la marca a la izquierda y el formulario a la derecha
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.widthIn(max = FORM_MAX_WIDTH)
                ) {
                    AuthBrandHeader(onColoredBackground = true)
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = FORM_MAX_WIDTH)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { form() }
            }
        }
    } else {
        // Celular: marca arriba y formulario debajo
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthBrandHeader(onColoredBackground = false)
            Spacer(modifier = Modifier.height(4.dp))
            form()
        }
    }
}

/** Ancho máximo del formulario para que no se estire en pantallas grandes. */
private val FORM_MAX_WIDTH = 420.dp

/** Logo, nombre de bienvenida y subtítulo de la app. */
@Composable
private fun AuthBrandHeader(onColoredBackground: Boolean) {
    val titleColor = if (onColoredBackground) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (onColoredBackground) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
    Icon(
        Icons.Default.Storefront,
        contentDescription = null,
        modifier = Modifier.size(if (onColoredBackground) 96.dp else 64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(R.string.auth_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = titleColor,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.auth_welcome_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = subtitleColor,
        textAlign = TextAlign.Center
    )
}

/** Campo de contraseña con un ojo para mostrarla u ocultarla. */
@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, enabled: Boolean) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(if (visible) R.string.auth_hide_password else R.string.auth_show_password)
                )
            }
        },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}
