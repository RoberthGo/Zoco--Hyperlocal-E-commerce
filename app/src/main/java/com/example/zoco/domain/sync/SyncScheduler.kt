package com.example.zoco.domain.sync

/**
 * Contrato para pedir que los cambios pendientes (creados/editados sin conexión)
 * se suban a la Web API en cuanto haya red. La implementación real usa WorkManager;
 * en pruebas se puede reemplazar por un falso.
 */
interface SyncScheduler {
    fun scheduleSync()
}
