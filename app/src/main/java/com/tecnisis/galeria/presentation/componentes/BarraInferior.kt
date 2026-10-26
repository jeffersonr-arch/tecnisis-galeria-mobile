package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class OpcionBarra(val etiqueta: String, val icono: ImageVector, val seleccionada: Boolean)

@Composable
fun BarraInferior(opciones: List<OpcionBarra>, onSeleccion: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = c.outlineVariant)
        NavigationBar(containerColor = c.surfaceContainerLowest, tonalElevation = 0.dp) {
            opciones.forEachIndexed { indice, opcion ->
                NavigationBarItem(
                    selected = opcion.seleccionada,
                    onClick = { onSeleccion(indice) },
                    icon = { Icon(opcion.icono, contentDescription = null) },
                    label = {
                        Text(
                            opcion.etiqueta,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.onPrimaryContainer,
                        selectedTextColor = c.onSurface,
                        indicatorColor = c.primaryContainer,
                        unselectedIconColor = c.onSurfaceVariant,
                        unselectedTextColor = c.onSurfaceVariant
                    )
                )
            }
        }
    }
}