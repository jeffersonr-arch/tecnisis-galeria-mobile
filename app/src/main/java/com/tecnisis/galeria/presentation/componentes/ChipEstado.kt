package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.domain.model.EstadoPostulacion

@Composable
fun ChipEstado(estado: EstadoPostulacion, modifier: Modifier = Modifier) {
    val (fondo, texto) = coloresDe(estado)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(iconoDe(estado), contentDescription = null, tint = texto, modifier = Modifier.size(14.dp))
        Text(estado.etiqueta, style = MaterialTheme.typography.labelMedium, color = texto)
    }
}

@Composable
private fun coloresDe(estado: EstadoPostulacion): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (estado) {
        EstadoPostulacion.VERIFICADA, EstadoPostulacion.APROBADA ->
            c.tertiaryContainer to c.onTertiaryContainer
        EstadoPostulacion.OBSERVADA -> c.secondaryContainer to c.onSecondaryContainer
        EstadoPostulacion.RECHAZADA -> c.errorContainer to c.onErrorContainer
        EstadoPostulacion.PENDIENTE_VERIFICACION -> c.surfaceVariant to c.onSurfaceVariant
    }
}

private fun iconoDe(estado: EstadoPostulacion): ImageVector = when (estado) {
    EstadoPostulacion.VERIFICADA, EstadoPostulacion.APROBADA -> Icons.Filled.Check
    EstadoPostulacion.OBSERVADA -> Icons.Outlined.Info
    EstadoPostulacion.RECHAZADA -> Icons.Filled.Close
    EstadoPostulacion.PENDIENTE_VERIFICACION -> Icons.Outlined.Schedule
}