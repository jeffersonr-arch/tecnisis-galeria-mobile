package com.tecnisis.galeria.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tecnisis.galeria.GaleriaApplication
import com.tecnisis.galeria.MainActivity
import com.tecnisis.galeria.R
import com.tecnisis.galeria.domain.model.AvisoDecision
import com.tecnisis.galeria.domain.model.EXTRA_ABRIR_MIS_OBRAS
import com.tecnisis.galeria.domain.model.avisoDecision
import com.tecnisis.galeria.domain.model.avisoTasacion
import com.tecnisis.galeria.domain.model.idAvisoTasacion
import com.tecnisis.galeria.domain.model.avisoVeredicto
import com.tecnisis.galeria.domain.model.idAvisoVeredicto

/**
 * HU07: recibe los mensajes de Firebase Cloud Messaging. La Cloud Function manda un mensaje de "datos"
 * (no de notificación): así este código corre siempre, con la app abierta o cerrada.
 * HU08: también llega "veredicto" (una obra aceptada o rechazada), con una notificación por obra.
 * HU10: y "tasacion" (la obra quedó lista para exhibición), también una por obra.
 */
class MensajeriaService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        (application as GaleriaApplication).container.guardarToken(token)
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos = mensaje.data
        val aviso: AvisoDecision
        val idNotificacion: Int
        when (datos["tipo"]) {
            "decision" -> {
                aviso = avisoDecision(datos["estado"] == "aprobada", datos["cantidad"]?.toIntOrNull() ?: 0)
                idNotificacion = ID_DECISION
            }
            "veredicto" -> {
                aviso = avisoVeredicto(datos["estado"] == "aceptada", datos["titulo"].orEmpty())
                idNotificacion = idAvisoVeredicto(datos["obraId"].orEmpty()) // uno por obra: no se reemplazan
            }
            "tasacion" -> { // HU10: el Especialista de Ventas tasó la obra
                aviso = avisoTasacion(datos["titulo"].orEmpty())
                idNotificacion = idAvisoTasacion(datos["obraId"].orEmpty())
            }
            else -> return
        }
        // 1) Baja la decisión o el veredicto a Room con WorkManager, aunque el socio haya negado las notificaciones
        (application as GaleriaApplication).container.programarSeguimiento()
        // 2) Muestra el aviso (si el socio dio el permiso)
        mostrarAviso(aviso, idNotificacion)
    }

    private fun mostrarAviso(aviso: AvisoDecision, idNotificacion: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Solicitudes de exhibición", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
        // Desde Android 13 hace falta el permiso; si el socio lo negó, no se muestra (la decisión igual llega a Room)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        // Al tocarla se abre la app en Mis obras
        val abrir = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ABRIR_MIS_OBRAS, true)
        }
        val alTocar = PendingIntent.getActivity(
            this, 0, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.logo_tecnisis)
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(idNotificacion, notificacion)
    }

    private companion object {
        const val CANAL = "solicitudes"
        const val ID_DECISION = 2001
    }
}