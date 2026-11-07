package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.ObraError

/** HU06: WorkManager lo corre cuando hay internet y sube las solicitudes "Pendiente de envío". */
class EnvioSolicitudWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val subir = (applicationContext as GaleriaApplication).container.subirSolicitudes

        return when (subir().exceptionOrNull()) {
            null -> Result.success()
            // Se cayó la red: WorkManager reintenta con espera creciente (30 s, 1 min, 2 min...)
            ObraError.SinConexion -> if (runAttemptCount < MAX_INTENTOS) Result.retry() else Result.failure()
            // Reglas o archivo: reintentar no lo arregla. El detalle queda en Logcat (EnvioSolicitud)
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_INTENTOS = 8
    }
}