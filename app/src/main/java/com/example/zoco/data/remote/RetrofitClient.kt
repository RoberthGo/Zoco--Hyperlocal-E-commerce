package com.example.zoco.data.remote

import com.example.zoco.data.remote.api.NotificationApiService
import com.example.zoco.data.remote.api.OrderApiService
import com.example.zoco.data.remote.api.ProductApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // IP actual del host en la red Wi-Fi
    const val CURRENT_WIFI_URL = "http://192.168.20.180:8000/api/v1/"
    const val LOCALHOST_URL = "http://127.0.0.1:8000/api/v1/"
    const val EMULATOR_LOOPBACK_URL = "http://10.0.2.2:8000/api/v1/"
    const val PREVIOUS_WIFI_URL = "http://192.168.1.147:8000/api/v1/"

    const val DEFAULT_BASE_URL = CURRENT_WIFI_URL

    val CANDIDATE_URLS = listOf(
        CURRENT_WIFI_URL,
        LOCALHOST_URL,
        EMULATOR_LOOPBACK_URL,
        PREVIOUS_WIFI_URL
    )

    @Volatile
    var currentBaseUrl: String = DEFAULT_BASE_URL
        private set

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var retrofitInstance: Retrofit = createRetrofit(DEFAULT_BASE_URL)

    @Volatile
    var productApiService: ProductApiService = retrofitInstance.create(ProductApiService::class.java)
        private set

    @Volatile
    var orderApiService: OrderApiService = retrofitInstance.create(OrderApiService::class.java)
        private set

    @Volatile
    var notificationApiService: NotificationApiService = retrofitInstance.create(NotificationApiService::class.java)
        private set

    fun createRetrofit(baseUrl: String): Retrofit {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Permite reconfigurar la URL base dinámicamente desde la interfaz (ej. desde Perfil).
     */
    fun updateBaseUrl(newUrl: String) {
        val safeUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        currentBaseUrl = safeUrl
        retrofitInstance = createRetrofit(safeUrl)
        productApiService = retrofitInstance.create(ProductApiService::class.java)
        orderApiService = retrofitInstance.create(OrderApiService::class.java)
        notificationApiService = retrofitInstance.create(NotificationApiService::class.java)
    }

    /**
     * Prueba si el backend Django responde exitosamente y retorna el resultado detallado.
     */
    suspend fun pingBackend(): Pair<Boolean, String> {
        return try {
            val products = productApiService.getProducts()
            Pair(true, "¡Conectado exitosamente con la Web API! Se recibieron ${products.size} productos.")
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            Pair(false, "No se pudo conectar a $currentBaseUrl ($errorMsg)")
        }
    }
}
