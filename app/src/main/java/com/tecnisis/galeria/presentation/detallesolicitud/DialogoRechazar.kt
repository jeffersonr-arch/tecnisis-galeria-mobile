package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * HU07: rechazar exige un motivo (1 a [maximo] caracteres). Si alguna obra no tiene Experto,
 * [sugerencia] ofrece un texto listo para usarlo con un toque.
 */
@Composable
fun DialogoRechazar(
    cantidadObras: Int,
    motivo: String,
    maximo: Int,
    mostrarError: Boolean,
    sugerencia: String?,
    onMotivo: (String) -> Unit,
    onRechazar: () -> Unit,
    onCancelar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val obras = if (cantidadObras == 1) "La obra quedará como no admitida."
    else "Las $cantidadObras obras quedarán como no admitidas."
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = c.surfaceContainerLowest,
        icon = { Icon(Icons.Outlined.Cancel, contentDescription = null, tint = c.error) },
        title = {
            Text("¿Rechazar la solicitud?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$obras Se generará el documento de rechazo por duplicado y el socio podrá armar una solicitud nueva.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= maximo) onMotivo(it) },
                    label = { Text("Motivo del rechazo (obligatorio)") },
                    placeholder = { Text("Explica al socio por qué se rechaza") },
                    isError = mostrarError,
                    minLines = 3,
                    supportingText = {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                if (mostrarError) "Escribe el motivo del rechazo" else "",
                                color = c.error,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${motivo.length} / $maximo")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (sugerencia != null && motivo != sugerencia) {
                    OutlinedButton(onClick = { onMotivo(sugerencia) }) {
                        Text("Usar: “$sugerencia”", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRechazar,
                colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError)
            ) { Text("Rechazar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Preview(name = "Motivo vacío · claro", showBackground = true)
@Preview(name = "Motivo vacío · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoRechazarPreview() {
    TecnisisGaleriaTheme {
        DialogoRechazar(
            cantidadObras = 3, motivo = "", maximo = 500, mostrarError = true,
            sugerencia = "No hay Experto disponible para Textil",
            onMotivo = {}, onRechazar = {}, onCancelar = {}
        )
    }
}