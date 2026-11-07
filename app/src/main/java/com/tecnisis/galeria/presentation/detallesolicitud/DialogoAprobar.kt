package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/** HU07: una línea del resumen del diálogo: la obra y el Experto que la evaluará. */
data class FilaAprobacion(val obra: String, val experto: String)

@Composable
fun DialogoAprobar(
    nombreSocio: String,
    filas: List<FilaAprobacion>,
    onAprobar: () -> Unit,
    onCancelar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = c.surfaceContainerLowest,
        icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.tertiary) },
        title = {
            Text("¿Aprobar la solicitud?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Cada obra pasará a su Experto para la evaluación artística. Se generará el documento " +
                            "de aprobación por duplicado y $nombreSocio recibirá una notificación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
                Surface(color = c.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filas.forEach { fila ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    fila.obra,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(fila.experto, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onAprobar) { Text("Aprobar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoAprobarPreview() {
    TecnisisGaleriaTheme {
        DialogoAprobar(
            nombreSocio = "Rosa Quispe Mamani",
            filas = listOf(
                FilaAprobacion("Mañana en Paracas", "Carmen Villanueva"),
                FilaAprobacion("Telar de Ocongate", "Jorge Huamán Ccori"),
                FilaAprobacion("Vasija nocturna", "Lucía Paredes Rojas")
            ),
            onAprobar = {}, onCancelar = {}
        )
    }
}