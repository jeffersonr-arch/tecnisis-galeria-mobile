package com.tecnisis.galeria.presentation.componentes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/**
 * HU07: una fila para abrir un PDF (sello "PDF", título, nombre del archivo e ícono).
 * Mientras se descarga, el ícono cambia por una ruedita y la fila no responde.
 */
@Composable
fun FilaPdf(
    titulo: String,
    subtitulo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    icono: ImageVector = Icons.AutoMirrored.Outlined.OpenInNew,
    fondo: Color = MaterialTheme.colorScheme.surfaceContainerLowest
) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = !cargando,
        color = fondo,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = c.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.size(width = 36.dp, height = 40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = c.onErrorContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            if (cargando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(icono, contentDescription = "Abrir", tint = c.outline, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilaPdfPreview() {
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilaPdf("Copia · Archivo de la galería", "archivo.pdf", onClick = {})
                FilaPdf("Copia · Artista", "artista.pdf", onClick = {}, cargando = true)
                FilaPdf(
                    "Mi documento de aprobación", null, onClick = {},
                    icono = Icons.Outlined.FileDownload, fondo = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}