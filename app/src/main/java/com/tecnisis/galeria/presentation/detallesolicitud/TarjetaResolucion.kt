package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.componentes.FilaPdf
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU07 · Parte 2: lo que ve el Anfitrión en una solicitud ya resuelta: quién decidió y cuándo,
 * el motivo (si la rechazó) y las 2 copias del documento.
 */
@Composable
fun TarjetaResolucion(
    aprobada: Boolean,
    fecha: String?,
    motivo: String?,
    abriendoArchivo: Boolean,
    abriendoArtista: Boolean,
    onAbrirArchivo: () -> Unit,
    onAbrirArtista: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val color = if (aprobada) c.tertiary else c.error
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (aprobada) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = null, tint = color, modifier = Modifier.size(20.dp)
            )
            Text(
                (if (aprobada) "Aprobada por ti" else "Rechazada por ti") +
                        (fecha?.let { " el $it" } ?: ""),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        if (motivo != null) {
            Surface(
                color = c.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MOTIVO DEL RECHAZO", style = MaterialTheme.typography.labelSmall, color = c.onErrorContainer)
                    Text(motivo, style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
                }
            }
        }
        TituloSeccion("Documento por duplicado", Modifier.padding(top = 16.dp))
        FilaPdf("Copia · Archivo de la galería", "archivo.pdf", onClick = onAbrirArchivo, cargando = abriendoArchivo)
        HorizontalDivider(color = c.outlineVariant)
        FilaPdf("Copia · Artista", "artista.pdf", onClick = onAbrirArtista, cargando = abriendoArtista)
    }
}

@Preview(name = "Aprobada · claro", showBackground = true)
@Preview(name = "Aprobada · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TarjetaResolucionAprobadaPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                TarjetaResolucion(true, "13 nov · 9:10 a. m.", null, false, false, {}, {})
            }
        }
    }
}

@Preview(name = "Rechazada", showBackground = true)
@Composable
private fun TarjetaResolucionRechazadaPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                TarjetaResolucion(
                    false, "13 nov · 9:10 a. m.", "No hay Experto disponible para Textil",
                    abriendoArchivo = false, abriendoArtista = true,
                    onAbrirArchivo = {}, onAbrirArtista = {}
                )
            }
        }
    }
}