package com.example.zoco.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf("es") }
    var apiUrl by remember { mutableStateOf(RetrofitClient.currentBaseUrl) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>("Listo para probar conexión con Django REST API") }
    var isOnline by remember { mutableStateOf<Boolean?>(null) }
    val chipScrollState = rememberScrollState()

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

        // Tarjeta de Usuario
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = stringResource(R.string.user_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.user_role),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
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
                        text = "Conexión Web API (Retrofit)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "URL base del backend Django REST:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = {
                        apiUrl = it
                        RetrofitClient.updateBaseUrl(it)
                    },
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
                        onClick = {
                            apiUrl = RetrofitClient.DEFAULT_BASE_URL
                            RetrofitClient.updateBaseUrl(apiUrl)
                        },
                        label = { Text("Wi-Fi PC (192.168.1.147)") }
                    )
                    AssistChip(
                        onClick = {
                            apiUrl = RetrofitClient.EMULATOR_LOOPBACK_URL
                            RetrofitClient.updateBaseUrl(apiUrl)
                        },
                        label = { Text("Emulador (10.0.2.2)") }
                    )
                    AssistChip(
                        onClick = {
                            apiUrl = RetrofitClient.LOCALHOST_URL
                            RetrofitClient.updateBaseUrl(apiUrl)
                        },
                        label = { Text("Localhost (127.0.0.1)") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isCheckingConnection = true
                                connectionStatus = "Probando conexión con Django REST API..."
                                val (ok, msg) = RetrofitClient.pingBackend()
                                isOnline = ok
                                isCheckingConnection = false
                                connectionStatus = msg
                            }
                        },
                        enabled = !isCheckingConnection
                    ) {
                        if (isCheckingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Text("Probar Conexión")
                    }

                    if (isOnline != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOnline == true) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (isOnline == true) "En Línea" else "Offline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (connectionStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionStatus ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                        selected = selectedLanguage == "es",
                        onClick = { selectedLanguage = "es" }
                    )
                    Text(stringResource(R.string.language_es), modifier = Modifier.padding(end = 16.dp))

                    RadioButton(
                        selected = selectedLanguage == "en",
                        onClick = { selectedLanguage = "en" }
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
                        text = "Equipo de Desarrollo Zoco",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Roberto Gordillo Herrera\n2. Leonardo Nuñez Zavala\n3. Ricardo Abraham Pérez Hernández",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
