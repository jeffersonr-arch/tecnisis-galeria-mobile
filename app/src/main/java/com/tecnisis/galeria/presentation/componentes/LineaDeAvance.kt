package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

enum class EstadoPaso { COMPLETO, ACTUAL, PENDIENTE }

data class PasoAvance(val titulo: String, val detalle: String, val estado: EstadoPaso)

@Composable
fun LineaDeAvance(pasos: List<PasoAvance>, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier) {
        pasos.forEachIndexed { i, paso ->
            val esUltimo = i == pasos.lastIndex
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Circulo(paso.estado)
                    if (!esUltimo) {
                        val tramoCompleto = paso.estado == EstadoPaso.COMPLETO &&
                                pasos[i + 1].estado == EstadoPaso.COMPLETO
                        Box(
                            Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(if (tramoCompleto) c.primary else c.outlineVariant)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(bottom = if (esUltimo) 0.dp else 16.dp)) {
                    Text(
                        paso.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (paso.estado == EstadoPaso.PENDIENTE) c.onSurfaceVariant else c.onSurface
                    )
                    Text(
                        paso.detalle,
                        style = if (paso.estado == EstadoPaso.ACTUAL) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.bodySmall,
                        color = if (paso.estado == EstadoPaso.ACTUAL) c.onSecondaryContainer else c.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Circulo(estado: EstadoPaso) {
    val c = MaterialTheme.colorScheme
    val base = Modifier.size(26.dp).clip(CircleShape)
    when (estado) {
        EstadoPaso.COMPLETO -> Box(base.background(c.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(15.dp))
        }
        EstadoPaso.ACTUAL -> Box(
            base.background(c.secondaryContainer).border(2.5.dp, c.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.secondary))
        }
        EstadoPaso.PENDIENTE -> Box(base.border(2.dp, c.outlineVariant, CircleShape))
    }
}