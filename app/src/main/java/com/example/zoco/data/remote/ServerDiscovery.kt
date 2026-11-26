package com.example.zoco.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

/**
 * Encuentra solo la URL del backend para no tener que teclear IPs:
 *  - emulador: 10.0.2.2
 *  - teléfono por USB con `adb reverse tcp:8000 tcp:8000`: 127.0.0.1
 *  - teléfono en hotspot de la laptop: la puerta de enlace del Wi-Fi (la laptop)
 *  - la última URL que funcionó o que el usuario escribió en Perfil
 * Prueba todas a la vez y se queda con la primera que responde, en orden de prioridad.
 */
class ServerDiscovery(
    context: Context,
    private val prefs: SharedPreferences?
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    /** Cliente HTTP que solo sirve para "tocar la puerta" del servidor con tiempos de espera cortos. */
    private fun buildProbeClient(connectTimeoutMs: Long) = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val probeClient = buildProbeClient(connectTimeoutMs = 1500)

    /** Para el escaneo de las 254 direcciones de la red: espera menos por cada una. */
    private val fastProbe = buildProbeClient(connectTimeoutMs = 700)

    /** Carga la última URL guardada para que la primera petición ya salga bien. */
    fun restoreSavedUrl() {
        prefs?.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }?.let {
            RetrofitClient.updateBaseUrl(it)
        }
    }

    /** Si la URL actual ya responde no hace nada; si no, busca una que sí. Devuelve true si hay servidor. */
    suspend fun findServer(): Boolean = mutex.withLock {
        if (isReachable(RetrofitClient.currentBaseUrl)) return true
        val found = detect()
        if (found != null) {
            RetrofitClient.updateBaseUrl(found)
            save(found)
            true
        } else {
            false
        }
    }

    fun save(url: String) {
        prefs?.edit()?.putString(KEY_BASE_URL, url.asBaseUrl())?.apply()
    }

    private suspend fun detect(): String? = coroutineScope {
        val candidates = candidates()
        Log.d(TAG, "Probando servidores: $candidates")
        val results = candidates.map { url -> async(Dispatchers.IO) { url to isReachable(url) } }.awaitAll()
        val direct = results.firstOrNull { it.second }?.first
        if (direct != null) {
            Log.d(TAG, "Servidor encontrado: $direct")
            direct
        } else {
            scanLocalNetwork().also { Log.d(TAG, "Escaneo de red local: $it") }
        }
    }

    /** Último recurso (Wi-Fi de router): prueba las 254 direcciones de la misma red /24 del teléfono. */
    private suspend fun scanLocalNetwork(): String? = coroutineScope {
        val prefix = localSubnetPrefix() ?: return@coroutineScope null
        val results = (1..254).map { host ->
            async(Dispatchers.IO) {
                val url = url("$prefix.$host")
                url to isReachable(url, fastProbe)
            }
        }.awaitAll()
        results.firstOrNull { it.second }?.first
    }

    private fun localSubnetPrefix(): String? {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
            props.linkAddresses
                .map { it.address }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress?.substringBeforeLast('.')
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun isReachable(baseUrl: String, client: OkHttpClient = probeClient): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.asBaseUrl()
            client.newCall(Request.Builder().url("${url}products").build()).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun candidates(): List<String> {
        val list = mutableListOf<String>()
        if (isEmulator()) list += url("10.0.2.2")
        list += url("127.0.0.1")
        gatewayIp()?.let { list += url(it) }
        prefs?.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }?.let { list += it }
        if (!isEmulator()) list += url("10.0.2.2")
        list += RetrofitClient.CANDIDATE_URLS
        return list.map { it.asBaseUrl() }.distinct()
    }

    private fun url(host: String) = "http://$host:8000/api/v1/"

    private fun gatewayIp(): String? {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
            props.routes
                .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                ?.gateway?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
    }

    companion object {
        private const val TAG = "ServerDiscovery"
        const val KEY_BASE_URL = "api_base_url"
        private const val READ_TIMEOUT_MS = 1500L
        private const val CALL_TIMEOUT_MS = 2500L
    }
}
