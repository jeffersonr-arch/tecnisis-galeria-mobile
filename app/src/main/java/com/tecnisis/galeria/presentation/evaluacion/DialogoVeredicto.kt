package com.tecnisis.galeria.presentation.evaluacion

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU08: confirmar el veredicto de una obra. Al rechazar el comentario es obligatorio; al aceptar, opcional.
 * Mientras se guarda ([guardando]) los botones se apagan y el diálogo no se cierra: así un doble toque no
 * manda dos veredictos. Si falla, [error] se muestra dentro del diálogo, que sigue abierto.
 */
@Composable
fun DialogoVeredicto(
    rechazar: Boolean,
    tituloObra: String,
    nombreSocio: String,
    comentario: String,
    maximo: Int,
    mostrarError: Boolean,
    guardando: Boolean,
    error: String?,
    onComentario: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        containerColor = c.surfaceContainerLowest,
        icon = {
            if (rechazar) Icon(Icons.Outlined.Cancel, contentDescription = null, tint = c.error)
            else Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.tertiary)
        },
        title = {
            Text(
                if (rechazar) "¿Rechazar la obra?" else "¿Aceptar la obra?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (rechazar) "“$tituloObra” no pasará a la evaluación económica. El veredicto es definitivo y el socio verá tu comentario."
                    else "“$tituloObra” pasará a la evaluación económica. El veredicto es definitivo y $nombreSocio recibirá una notificación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { if (it.length <= maximo) onComentario(it) },
                    enabled = !guardando,
                    label = { Text(if (rechazar) "Comentario (obligatorio)" else "Comentario (opcional)") },
                    placeholder = {
                        Text(if (rechazar) "Explica al socio por qué se rechaza" else "Algo que quieras dejar registrado")
                    },
                    isError = mostrarError,
                    minLines = 3,
                    supportingText = {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                if (mostrarError) "Escribe el motivo del rechazo" else "",
                                color = c.error,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${comentario.length} / $maximo")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = c.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !guardando,
                colors = if (rechazar) ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError)
                else ButtonDefaults.buttonColors()
            ) { Text(if (guardando) "Guardando…" else if (rechazar) "Rechazar" else "Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } }
    )
}

@Preview(name = "Aceptar · claro", showBackground = true)
@Preview(name = "Aceptar · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoAceptarPreview() {
    TecnisisGaleriaTheme {
        DialogoVeredicto(
            rechazar = false, tituloObra = "Mañana en Paracas", nombreSocio = "Rosa Quispe Mamani",
            comentario = "", maximo = 500, mostrarError = false, guardando = false, error = null,
            onComentario = {}, onConfirmar = {}, onCancelar = {}
        )
    }
}

@Preview(name = "Rechazar vacío · claro", showBackground = true)
@Preview(name = "Rechazar vacío · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoRechazarVacioPreview() {
    TecnisisGaleriaTheme {
        DialogoVeredicto(
            rechazar = true, tituloObra = "Mañana en Paracas", nombreSocio = "Rosa Quispe Mamani",
            comentario = "", maximo = 500, mostrarError = true, guardando = false, error = null,
            onComentario = {}, onConfirmar = {}, onCancelar = {}
        )
    }
}

@Preview(name = "Error sin internet", showBackground = true)
@Composable
private fun DialogoSinInternetPreview() {
    TecnisisGaleriaTheme {
        DialogoVeredicto(
            rechazar = true, tituloObra = "Mañana en Paracas", nombreSocio = "Rosa Quispe Mamani",
            comentario = "La pintura presenta craquelado en la esquina inferior.", maximo = 500,
            mostrarError = false, guardando = false,
            error = "Necesitas internet para registrar el veredicto. No se guardó nada.",
            onComentario = {}, onConfirmar = {}, onCancelar = {}
        )
    }
}