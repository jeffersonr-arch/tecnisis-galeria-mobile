package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TarjetaFuncion(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    fondoIcono: Color,
    colorIcono: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    val habilitada = onClick != null

    Surface(
        onClick = { onClick?.invoke() },
        enabled = habilitada,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = MaterialTheme.shapes.large,
        color = if (habilitada) c.surfaceContainerLowest else Color.Transparent,
        border = BorderStroke(1.dp, c.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (habilitada) fondoIcono else c.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = if (habilitada) colorIcono else c.outline)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (habilitada) c.onSurface else c.onSurfaceVariant
                )
                if (subtitulo.isNotBlank()) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            if (habilitada) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.outline)
            } else {
                Text(
                    "Pronto",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.surfaceVariant)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}