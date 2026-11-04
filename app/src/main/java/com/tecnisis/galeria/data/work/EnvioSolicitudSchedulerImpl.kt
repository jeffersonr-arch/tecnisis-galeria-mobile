package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tecnisis.galeria.domain.repository.EnvioSolicitudScheduler
import java.util.concurrent.TimeUnit

class EnvioSolicitudSchedulerImpl(private val contexto: Context) : EnvioSolicitudScheduler {

    override fun programar() {
        val solicitud = OneTimeWorkRequestBuilder<EnvioSolicitudWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // APPEND_OR_REPLACE: si ya hay un envío en curso, este va detrás y no se pierde
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork(TRABAJO, ExistingWorkPolicy.APPEND_OR_REPLACE, solicitud)
    }

    private companion object {
        const val TRABAJO = "enviar_solicitudes"
    }
}