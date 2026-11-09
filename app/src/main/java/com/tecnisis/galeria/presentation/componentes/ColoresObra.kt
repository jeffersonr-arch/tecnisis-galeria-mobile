package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU08: colores del chip de cada obra (fondo y texto), los mismos en todas las pantallas:
 * en evaluación = cobalto, aceptada = esmeralda, rechazada o no admitida = rojo.
 */
@Composable
fun coloresEstadoObra(estado: EstadoObra): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (estado) {
        EstadoObra.EN_EVALUACION -> c.primaryContainer to c.onPrimaryContainer
        EstadoObra.ACEPTADA, EstadoObra.LISTA_EXHIBICION -> c.tertiaryContainer to c.onTertiaryContainer
        EstadoObra.RECHAZADA, EstadoObra.NO_ADMITIDA -> c.errorContainer to c.onErrorContainer
        EstadoObra.BORRADOR, EstadoObra.ENVIADA -> c.surfaceVariant to c.onSurfaceVariant
    }
}

/** El chip de una obra con su estado ("En evaluación", "Aceptada", "Rechazada"…). */
@Composable
fun ChipEstadoObra(estado: EstadoObra, modifier: Modifier = Modifier) {
    val (fondo, texto) = coloresEstadoObra(estado)
    ChipColor(estado.etiqueta, fondo, texto, modifier)
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChipEstadoObraPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipEstadoObra(EstadoObra.EN_EVALUACION)
                ChipEstadoObra(EstadoObra.ACEPTADA)
                ChipEstadoObra(EstadoObra.RECHAZADA)
                ChipEstadoObra(EstadoObra.NO_ADMITIDA)
            }
        }
    }
}