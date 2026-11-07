package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU07 · Parte 2: botones fijos abajo del detalle. Rechazar siempre se puede (con motivo);
 * Aprobar solo si todas las obras tienen Experto. "ayuda" explica por qué Aprobar está apagado.
 */
@Composable
fun BarraDecision(
    puedeAprobar: Boolean,
    ayuda: String?,
    ocupado: Boolean,
    onRechazar: () -> Unit,
    onAprobar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ayuda != null) {
                Text(
                    ayuda,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRechazar,
                    enabled = !ocupado,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.5.dp, if (ocupado) c.outlineVariant else c.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text("Rechazar", style = MaterialTheme.typography.titleSmall, color = if (ocupado) c.outline else c.error)
                }
                Button(
                    onClick = onAprobar,
                    enabled = puedeAprobar && !ocupado,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text("Aprobar", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Preview(name = "Puede aprobar · claro", showBackground = true)
@Preview(name = "Puede aprobar · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarraDecisionPreview() {
    TecnisisGaleriaTheme {
        BarraDecision(puedeAprobar = true, ayuda = null, ocupado = false, onRechazar = {}, onAprobar = {})
    }
}

@Preview(name = "Falta Experto", showBackground = true)
@Composable
private fun BarraDecisionSinExpertoPreview() {
    TecnisisGaleriaTheme {
        BarraDecision(
            puedeAprobar = false, ayuda = "Para aprobar, todas las obras deben tener Experto.",
            ocupado = false, onRechazar = {}, onAprobar = {}
        )
    }
}