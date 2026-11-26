package com.example.zoco.data.remote

import com.example.zoco.data.local.SessionManager
import com.example.zoco.data.remote.api.AuthApiService
import com.example.zoco.data.remote.api.NotificationApiService
import com.example.zoco.data.remote.api.OrderApiService
import com.example.zoco.data.remote.api.ProductApiService
import com.example.zoco.data.remote.api.UserProfileApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Punto único para hablar con el backend (Django). Reúne:
 *  - la URL actual del servidor, que puede cambiar mientras la app corre (ver [updateBaseUrl]);
 *  - los "servicios" de Retrofit (productos, pedidos, usuarios...), que se recrean al cambiar la URL;
 *  - el cliente HTTP, que agrega solo el token de sesión a cada petición y detecta sesiones vencidas.
 * Por eso los repositorios deben pedir el servicio cada vez (`RetrofitClient.productApiService`)
 * y no guardarlo en una variable por mucho tiempo.
 */
object RetrofitClient {

    // Direcciones típicas del backend en desarrollo. La app no depende de ninguna IP concreta:
    // ServerDiscovery prueba estas (y otras según la red) y se queda con la que responda.
    const val EMULATOR_LOOPBACK_URL = "http://10.0.2.2:8000/api/v1/"   // el emulador ve la PC como 10.0.2.2
    const val LOCALHOST_URL = "http://127.0.0.1:8000/api/v1/"         // teléfono por USB con `adb reverse`

    const val DEFAULT_BASE_URL = EMULATOR_LOOPBACK_URL

    /** Segundos que se espera al servidor antes de rendirse (la app funciona sin conexión, así que no hay que esperar mucho). */
    private const val TIMEOUT_SECONDS = 3L

    val CANDIDATE_URLS = listOf(EMULATOR_LOOPBACK_URL, LOCALHOST_URL)

    @Volatile
    var currentBaseUrl: String = DEFAULT_BASE_URL
        private set

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Se ejecuta en TODAS las peticiones: agrega el token de sesión (salvo en login/registro, que no lo necesitan)
     * y, si el servidor responde 401 (sesión vencida), avisa con [onUnauthorized].
     */
    private val authInterceptor = Interceptor { chain ->
        val token = SessionManager.activeToken
        val original = chain.request()
        val path = original.url.encodedPath
        val isPublicAuth = path.endsWith("auth/login/") || path.endsWith("auth/register/")
        val request = if (!token.isNullOrBlank() && !isPublicAuth) {
            original.newBuilder()
                .header("Authorization", "Token $token")
                .build()
        } else {
            original
        }
        val response = chain.proceed(request)
        // Token inválido o vencido en una ruta protegida: se cierra la sesión local.
        if (response.code == 401 && !isPublicAuth && !token.isNullOrBlank()) {
            onUnauthorized?.invoke()
        }
        response
    }

    /** Callback de la capa de sesión: un 401 con token devuelve al usuario a la pantalla de login. */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /** Los servicios de Retrofit que apuntan a la URL actual. Se reemplazan todos juntos al cambiar de URL. */
    private class Services(retrofit: Retrofit) {
        val product: ProductApiService = retrofit.create(ProductApiService::class.java)
        val order: OrderApiService = retrofit.create(OrderApiService::class.java)
        val notification: NotificationApiService = retrofit.create(NotificationApiService::class.java)
        val userProfile: UserProfileApiService = retrofit.create(UserProfileApiService::class.java)
        val auth: AuthApiService = retrofit.create(AuthApiService::class.java)
    }

    @Volatile
    private var services = Services(buildRetrofit(DEFAULT_BASE_URL))

    val productApiService: ProductApiService get() = services.product
    val orderApiService: OrderApiService get() = services.order
    val notificationApiService: NotificationApiService get() = services.notification
    val userProfileApiService: UserProfileApiService get() = services.userProfile
    val authApiService: AuthApiService get() = services.auth

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl.asBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    /** Cambia la URL del servidor (la usa [ServerDiscovery] al encontrar el backend). */
    fun updateBaseUrl(newUrl: String) {
        val safeUrl = newUrl.asBaseUrl()
        currentBaseUrl = safeUrl
        services = Services(buildRetrofit(safeUrl))
    }
}
