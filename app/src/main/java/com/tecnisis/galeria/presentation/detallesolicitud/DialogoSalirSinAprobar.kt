package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun DialogoSalirSinAprobar(onSalir: () -> Unit, onQuedarse: () -> Unit) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onQuedarse,
        containerColor = c.surfaceContainerLowest,
        title = { Text("¿Salir sin aprobar?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "Se perderán los cambios de Experto que hiciste. La próxima vez verás la recomendación original.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onSalir) { Text("Salir", color = c.error) }
        },
        dismissButton = {
            TextButton(onClick = onQuedarse) { Text("Seguir revisando") }
        }
    )
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogoSalirSinAprobarPreview() {
    TecnisisGaleriaTheme {
        DialogoSalirSinAprobar(onSalir = {}, onQuedarse = {})
    }
}