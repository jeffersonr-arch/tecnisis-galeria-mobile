package com.tecnisis.galeria.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tecnisis.galeria.domain.repository.VerificacionScheduler
import java.util.concurrent.TimeUnit

class VerificacionSchedulerImpl(private val contexto: Context) : VerificacionScheduler {

    override fun programar(uid: String) {
        val solicitud = OneTimeWorkRequestBuilder<VerificacionWorker>()
            .setInputData(workDataOf(VerificacionWorker.CLAVE_UID to uid))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(contexto)
            .enqueueUniqueWork("verificacion_$uid", ExistingWorkPolicy.KEEP, solicitud)
    }
}