package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.domain.model.PostulacionError
import kotlinx.coroutines.CancellationException
import java.io.IOException

class VerificacionWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(CLAVE_UID) ?: return Result.failure()
        val verificar = (applicationContext as GaleriaApplication).container.verificarPostulacion

        return try {
            val resultado = verificar(uid)
            if (resultado.exceptionOrNull() is PostulacionError.SinConexion) Result.retry() else Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Sin internet o RENIEC caído: WorkManager reintenta con espera creciente
            if (runAttemptCount < MAX_INTENTOS) {
                Result.retry()
            } else {
                verificar.enviarARevisionManual(uid, "No se pudo consultar RENIEC tras varios intentos")
                Result.success()
            }
        } catch (e: Exception) {
            // Token inválido u otro error de la API: pasa a revisión humana
            verificar.enviarARevisionManual(uid, "Verificación automática no disponible: ${e.message}")
            Result.success()
        }
    }

    companion object {
        const val CLAVE_UID = "uid"
        private const val MAX_INTENTOS = 5
    }
}