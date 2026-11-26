package com.example.zoco.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zoco.ZocoApplication

/**
 * Tarea en segundo plano que sube a la Web API lo creado/editado sin conexión.
 * WorkManager solo la ejecuta cuando hay red (constraint CONNECTED), incluso con la app cerrada.
 * Obtiene los repositorios del Service Locator de [ZocoApplication] (misma DI manual del proyecto).
 */
class SyncPendingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ZocoApplication

        val productsOk = app.productRepository.pushPending()
        val ordersOk = app.orderRepository.pushPending()

        return if (productsOk && ordersOk) {
            Log.d(TAG, "Cola de sincronización vaciada correctamente")
            Result.success()
        } else {
            // Quedó algo pendiente (servidor caído, IP incorrecta...): WorkManager reintenta con backoff
            Log.w(TAG, "Aún hay cambios pendientes; se reintentará más tarde")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncPendingWorker"
    }
}
