package com.example.zoco.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.ui.common.asString

/** Ancho máximo del formulario para que no se estire en pantallas grandes. */
private val FORM_MAX_WIDTH = 420.dp

/** Longitud mínima de la contraseña (el servidor también la exige). */
private const val MIN_PASSWORD_LENGTH = 6

/** Lo que la persona ha escrito en el formulario. Se conserva al girar la pantalla (ver [AuthFields.Saver]). */
private data class AuthFields(
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
) {
    companion object {
        val Saver = listSaver<AuthFields, String>(
            save = { listOf(it.username, it.fullName, it.email, it.password, it.confirmPassword) },
            restore = { AuthFields(it[0], it[1], it[2], it[3], it[4]) }
        )
    }
}

/**
 * Revisa lo escrito antes de llamar al servidor. Devuelve el texto del problema (`R.string...`)
 * o null si todo está bien.
 */
@StringRes
private fun validate(fields: AuthFields, isRegister: Boolean): Int? = when {
    !isRegister && (fields.username.isBlank() || fields.password.isBlank()) -> R.string.auth_error_required
    !isRegister -> null
    fields.username.isBlank() || fields.fullName.isBlank() || fields.email.isBlank() || fields.password.isBlank() ->
        R.string.auth_error_required
    !Patterns.EMAIL_ADDRESS.matcher(fields.email.trim()).matches() -> R.string.auth_error_email_invalid
    fields.password.length < MIN_PASSWORD_LENGTH -> R.string.auth_error_password_short
    fields.password != fields.confirmPassword -> R.string.auth_error_password_mismatch
    else -> null
}

/**
 * Pantalla de acceso obligatoria: pestañas Iniciar sesión / Registrarse.
 * El registro solo crea cuentas de comprador; los administradores únicamente inician sesión.
 *  - celular: logo arriba y formulario debajo.
 *  - plegable / tablet: panel de color con la marca a la izquierda y formulario a la derecha.
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
    var fields by rememberSaveable(stateSaver = AuthFields.Saver) { mutableStateOf(AuthFields()) }
    var localErrorRes by remember { mutableStateOf<Int?>(null) }
    val isRegister = tabIndex == 1

    fun submit() {
        localErrorRes = validate(fields, isRegister)
        if (localErrorRes != null) return
        if (isRegister) {
            onRegister(fields.username, fields.email, fields.password, fields.fullName)
        } else {
            onLogin(fields.username, fields.password)
        }
    }

    val form = @Composable {
        AuthForm(
            isRegister = isRegister,
            fields = fields,
            isLoading = uiState.isLoading,
            errorText = localErrorRes?.let { stringResource(it) } ?: uiState.errorMessage?.asString(),
            onTabSelected = { index ->
                tabIndex = index
                localErrorRes = null
                onClearError()
            },
            onFieldsChange = { fields = it },
            onSubmit = ::submit
        )
    }

    if (LocalWindowSize.current.hasRoomForTwoColumns) {
        SplitAuthLayout(form, modifier)
    } else {
        PhoneAuthLayout(form, modifier)
    }
}

/** Celular: marca arriba y formulario debajo, con desplazamiento. */
@Composable
private fun PhoneAuthLayout(form: @Composable () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AuthBrandHeader(onColoredBackground = false)
        form()
    }
}

/** Tablet o plegable: panel con la marca a la izquierda y el formulario a la derecha. */
@Composable
private fun SplitAuthLayout(form: @Composable () -> Unit, modifier: Modifier) {
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
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = FORM_MAX_WIDTH)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { form() }
        }
    }
}

/** Pestañas, campos, mensaje de error y botón. Se dibuja igual en todos los tamaños. */
@Composable
private fun AuthForm(
    isRegister: Boolean,
    fields: AuthFields,
    isLoading: Boolean,
    errorText: String?,
    onTabSelected: (Int) -> Unit,
    onFieldsChange: (AuthFields) -> Unit,
    onSubmit: () -> Unit
) {
    TabRow(selectedTabIndex = if (isRegister) 1 else 0) {
        Tab(selected = !isRegister, onClick = { onTabSelected(0) }, text = { Text(stringResource(R.string.auth_dialog_login_tab)) })
        Tab(selected = isRegister, onClick = { onTabSelected(1) }, text = { Text(stringResource(R.string.auth_dialog_register_tab)) })
    }

    AuthTextField(fields.username, { onFieldsChange(fields.copy(username = it)) }, R.string.auth_username_label, !isLoading)
    if (isRegister) {
        AuthTextField(fields.fullName, { onFieldsChange(fields.copy(fullName = it)) }, R.string.auth_fullname_label, !isLoading)
        AuthTextField(
            value = fields.email,
            onValueChange = { onFieldsChange(fields.copy(email = it)) },
            labelRes = R.string.auth_email_label,
            enabled = !isLoading,
            keyboardType = KeyboardType.Email
        )
    }
    PasswordField(fields.password, { onFieldsChange(fields.copy(password = it)) }, stringResource(R.string.auth_password_label), !isLoading)
    if (isRegister) {
        PasswordField(
            fields.confirmPassword,
            { onFieldsChange(fields.copy(confirmPassword = it)) },
            stringResource(R.string.auth_confirm_password_label),
            !isLoading
        )
        Text(
            text = stringResource(R.string.auth_register_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }

    errorText?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
    }

    Button(onClick = onSubmit, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(if (isRegister) R.string.auth_submit_register else R.string.auth_submit_login))
        }
    }
}

/** Campo de texto de una línea. */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

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
