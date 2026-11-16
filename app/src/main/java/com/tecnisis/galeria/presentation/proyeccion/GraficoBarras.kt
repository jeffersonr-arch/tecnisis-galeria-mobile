package com.tecnisis.galeria.presentation.proyeccion

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.tecnisis.galeria.domain.model.textoSoles
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/** Alto de cada barra con su espacio, y ancho fijo de la columna de nombres (igual en Por obra y Por socio). */
val ALTO_FILA_BARRA = 34.dp
private val ANCHO_ETIQUETAS = 124.dp

/**
 * Barras horizontales con MPAndroidChart: la primera de la lista va arriba. Barras en tertiary y «Otras» en outline;
 * el monto en soles va al final de cada barra. Sin leyenda, sin zoom, sin arrastre y sin resaltar al tocar.
 * En la vista previa de Android Studio se dibuja con Compose.
 */
@Composable
fun GraficoBarras(
    etiquetas: List<String>,
    centimos: List<Long>,
    esOtras: List<Boolean>,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    val colorBarra = c.tertiary
    val colorOtras = c.outline
    val colorValor = c.onSurface
    val colorEtiqueta = c.onSurfaceVariant
    val colorEje = c.outlineVariant
    val conDescripcion = modifier.semantics { contentDescription = descripcion }

    if (LocalInspectionMode.current) {
        BarrasVistaPrevia(etiquetas, centimos, esOtras, conDescripcion)
        return
    }

    val densidad = LocalDensity.current
    val izquierda = with(densidad) { ANCHO_ETIQUETAS.toPx() }
    val margen = with(densidad) { 8.dp.toPx() }

    AndroidView(
        modifier = conDescripcion,
        factory = { contexto ->
            HorizontalBarChart(contexto).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false) // sin zoom, arrastre ni resaltado
                setDrawGridBackground(false)
                setDrawValueAboveBar(true) // el monto va al final de la barra
                setNoDataText("")
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM // en las barras horizontales, BOTTOM es el lado izquierdo
                    setDrawGridLines(false)
                    granularity = 1f
                    textSize = 11f
                }
                // Columna de nombres de ancho fijo: las barras no cambian de tamaño al cambiar de chip
                setViewPortOffsets(izquierda, margen, margen, margen)
            }
        },
        update = { grafico ->
            val n = etiquetas.size
            // MPAndroidChart dibuja x = 0 abajo: la primera barra de la lista va con x = n − 1
            val entradas = (0 until n).map { x ->
                val i = n - 1 - x
                BarEntry(x.toFloat(), centimos[i] / 100f, centimos[i])
            }
            val datos = BarDataSet(entradas, "").apply {
                colors = (0 until n).map { x -> if (esOtras[n - 1 - x]) colorOtras.toArgb() else colorBarra.toArgb() }
                valueTextColor = colorValor.toArgb()
                valueTextSize = 11f
                isHighlightEnabled = false
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?): String =
                        textoSoles(barEntry?.data as? Long ?: 0L)
                }
            }
            val maximo = (centimos.maxOrNull() ?: 0L) / 100f
            grafico.axisLeft.axisMinimum = 0f
            grafico.axisLeft.axisMaximum = if (maximo > 0f) maximo * 1.7f else 1f // espacio para el monto
            grafico.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter((0 until n).map { x -> etiquetas[n - 1 - x] })
                setLabelCount(n, false)
                axisMinimum = -0.5f
                axisMaximum = n - 0.5f
                textColor = colorEtiqueta.toArgb()
                axisLineColor = colorEje.toArgb()
            }
            grafico.data = BarData(datos).apply { barWidth = 0.62f }
            grafico.invalidate()
        }
    )
}

/** Solo para la vista previa: las mismas barras dibujadas con Compose. */
@Composable
private fun BarrasVistaPrevia(etiquetas: List<String>, centimos: List<Long>, esOtras: List<Boolean>, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val maximo = (centimos.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        etiquetas.indices.forEach { i ->
            Row(Modifier.height(ALTO_FILA_BARRA - 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    etiquetas[i], style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.End,
                    modifier = Modifier.width(ANCHO_ETIQUETAS - 8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f * centimos[i] / maximo)
                            .height(18.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(if (esOtras[i]) c.outline else c.tertiary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(textoSoles(centimos[i]), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GraficoBarrasPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            GraficoBarras(
                etiquetas = listOf("Mañana, en Paracas", "Retablo de Sarhua", "Lluvia en Barranco", "Otras (3)"),
                centimos = listOf(135_000L, 112_000L, 37_515L, 35_500L),
                esOtras = listOf(false, false, false, true),
                descripcion = "Ganancia de la galería por obra",
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}