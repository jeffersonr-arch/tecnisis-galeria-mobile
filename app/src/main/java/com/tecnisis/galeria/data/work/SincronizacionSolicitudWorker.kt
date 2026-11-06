package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.ObraError

/** HU07: WorkManager lo corre cuando hay internet y baja a Room la decisión del Anfitrión. */
class SincronizacionSolicitudWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val sincronizar = (applicationContext as GaleriaApplication).container.sincronizarSolicitudes

        return when (sincronizar().exceptionOrNull()) {
            null -> Result.success()
            // Se cayó la red: WorkManager reintenta con espera creciente
            ObraError.SinConexion -> if (runAttemptCount < MAX_INTENTOS) Result.retry() else Result.failure()
            // Reglas u otro error: reintentar no lo arregla (el detalle queda en Logcat: SeguimientoSolicitud)
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_INTENTOS = 5
    }
}