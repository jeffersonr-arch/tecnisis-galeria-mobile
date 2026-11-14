package com.tecnisis.galeria.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.tecnisis.galeria.presentation.theme.galeria

/**
 * Dona de Aceptadas (tertiary) y Rechazadas (pigmentoRojo) con MPAndroidChart.
 * Se gira con el dedo y resalta la porción tocada. Sin etiquetas, valores ni leyenda de la librería: el centro y la leyenda los dibuja Compose.
 * En la vista previa de Android Studio se dibuja con Canvas.
 */
@Composable
fun GraficoDona(
    aceptadas: Int,
    rechazadas: Int,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    val colorAceptadas = MaterialTheme.colorScheme.tertiary
    val colorRechazadas = MaterialTheme.galeria.pigmentoRojo
    val colorHueco = MaterialTheme.colorScheme.surfaceContainerLowest
    val conDescripcion = modifier.semantics { contentDescription = descripcion }

    if (LocalInspectionMode.current) {
        Canvas(conDescripcion) {
            val total = (aceptadas + rechazadas).coerceAtLeast(1)
            val grosor = size.minDimension * 0.19f
            val lado = size.minDimension - grosor
            val esquina = Offset((size.width - lado) / 2, (size.height - lado) / 2)
            val barridoAceptadas = 360f * aceptadas / total
            drawArc(colorAceptadas, -90f, barridoAceptadas, false, esquina, Size(lado, lado), style = Stroke(grosor))
            drawArc(
                colorRechazadas, -90f + barridoAceptadas, 360f - barridoAceptadas, false,
                esquina, Size(lado, lado), style = Stroke(grosor)
            )
        }
        return
    }

    AndroidView(
        modifier = conDescripcion,
        factory = { contexto ->
            PieChart(contexto).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawEntryLabels(false)
                setDrawCenterText(false)
                isHighlightPerTapEnabled = true   // resalta una porción al tocarla
                isRotationEnabled = true          // se gira con el dedo
                isDrawHoleEnabled = true
                holeRadius = 62f
                transparentCircleRadius = 0f
                setNoDataText("")
            }
        },
        update = { grafico ->
            val ambas = aceptadas > 0 && rechazadas > 0
            val datos = PieDataSet(
                listOf(PieEntry(aceptadas.toFloat()), PieEntry(rechazadas.toFloat())), ""
            ).apply {
                colors = listOf(colorAceptadas.toArgb(), colorRechazadas.toArgb())
                setDrawValues(false)
                sliceSpace = if (ambas) 2f else 0f
                selectionShift = 6f               // cuánto sale la porción resaltada
            }
            grafico.setHoleColor(colorHueco.toArgb())
            grafico.data = PieData(datos)
            grafico.invalidate()
        }
    )
}