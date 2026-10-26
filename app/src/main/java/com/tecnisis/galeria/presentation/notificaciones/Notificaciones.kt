package com.tecnisis.galeria.presentation.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tecnisis.galeria.R

object Notificaciones {

    private const val CANAL_POSTULACIONES = "postulaciones"
    private const val ID_POSTULACION_RECIBIDA = 1001

    fun mostrarPostulacionRecibida(contexto: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_POSTULACIONES, "Postulaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }

        // Desde Android 13 hace falta el permiso; si el usuario lo negó, no se muestra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notificacion = NotificationCompat.Builder(contexto, CANAL_POSTULACIONES)
            .setSmallIcon(R.drawable.logo_tecnisis)
            .setContentTitle("Postulación recibida")
            .setContentText("Estamos verificando tus datos con RENIEC.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Estamos verificando tus datos con RENIEC. Revisa el resultado en \"Estado de mi postulación\"."
                )
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(contexto).notify(ID_POSTULACION_RECIBIDA, notificacion)
    }
}