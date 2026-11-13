package com.tecnisis.galeria.presentation.tasacion

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.domain.model.MontosTasacion
import com.tecnisis.galeria.domain.model.calcularMontos
import com.tecnisis.galeria.domain.model.textoSoles
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * Confirmar la tasación (es definitiva). Mientras se guarda ([guardando]) los botones se apagan y el
 * diálogo no se cierra: así un doble toque no manda dos tasaciones. Si falla, [error] se muestra dentro.
 * Con [yaTasada] (otra persona la tasó) solo queda el botón Volver.
 */
@Composable
fun DialogoTasacion(
    tituloObra: String,
    montos: MontosTasacion,
    guardando: Boolean,
    error: String?,
    yaTasada: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    onVolver: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        containerColor = c.surfaceContainerLowest,
        icon = { Icon(Icons.Outlined.Sell, contentDescription = null, tint = c.tertiary) },
        title = {
            Text("¿Guardar la tasación?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "“$tituloObra” pasará a Lista para exhibición. La tasación es definitiva y el socio recibirá su documento de venta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    FilaMonto("PVP", textoSoles(montos.pvpCentimos))
                    HorizontalDivider(color = c.outlineVariant)
                    FilaMonto("Galería (${montos.porcentaje} %)", textoSoles(montos.galeriaCentimos))
                    HorizontalDivider(color = c.outlineVariant)
                    FilaMonto("Socio recibe", textoSoles(montos.socioCentimos), destacado = true)
                }
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = c.error)
                }
            }
        },
        confirmButton = {
            if (yaTasada) {
                Button(onClick = onVolver) { Text("Volver") }
            } else {
                Button(onClick = onConfirmar, enabled = !guardando) { Text(if (guardando) "Guardando…" else "Guardar") }
            }
        },
        dismissButton = {
            if (!yaTasada) TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}

// ---------- Previews ----------

@Composable
private fun VistaPrevia(guardando: Boolean = false, error: String? = null, yaTasada: Boolean = false) {
    TecnisisGaleriaTheme {
        DialogoTasacion(
            tituloObra = "Mañana en Paracas", montos = calcularMontos(250_000, 18),
            guardando = guardando, error = error, yaTasada = yaTasada,
            onConfirmar = {}, onCancelar = {}, onVolver = {}
        )
    }
}

@Preview(name = "Confirmar · claro", showBackground = true)
@Preview(name = "Confirmar · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoConfirmarPreview() = VistaPrevia()

@Preview(name = "Guardando", showBackground = true)
@Composable
private fun DialogoGuardandoPreview() = VistaPrevia(guardando = true)

@Preview(name = "Error de subida", showBackground = true)
@Composable
private fun DialogoErrorPreview() =
    VistaPrevia(error = "No se pudo subir el documento de venta. No se guardó nada; inténtalo de nuevo.")

@Preview(name = "Ya tasada · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoYaTasadaPreview() =
    VistaPrevia(error = "Esta obra ya fue tasada. Vuelve a Por tasar.", yaTasada = true)