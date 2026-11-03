package com.tecnisis.galeria.presentation.obras

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.presentation.componentes.FotoObra
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

/** Avisos de la nota de voz. En todos, el socio puede escribir la nota a mano. */
enum class AvisoVoz(val texto: String) {
    SIN_VOZ("No te escuché. Toca Dictar e inténtalo otra vez."),
    FALLO("No se pudo reconocer la voz (necesita internet). Escribe la nota a mano."),
    SIN_PERMISO("Sin permiso del micrófono. Escribe la nota a mano."),
    NO_DISPONIBLE("Este celular no tiene reconocimiento de voz. Escribe la nota a mano.")
}

/** Ícono de color + título de cada tarjeta del formulario (como en el diseño). */
@Composable
internal fun EncabezadoBloque(
    icono: ImageVector,
    fondo: Color,
    color: Color,
    titulo: String,
    etiqueta: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fondo),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (etiqueta != null) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BloqueFoto(
    rutaFoto: String?,
    preparando: Boolean,
    conError: Boolean,
    onTomarFoto: () -> Unit,
    onElegirGaleria: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(modifier) {
        EncabezadoBloque(Icons.Outlined.PhotoCamera, c.primaryContainer, c.onPrimaryContainer, "Fotografía", "Obligatoria")
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = if (conError) 2.dp else 1.dp,
                    color = if (conError) c.error else c.outlineVariant,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rutaFoto != null) {
                FotoObra(rutaFoto, Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Agrega una foto de la obra", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
            if (preparando) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(c.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }
        if (conError) {
            Text(
                "La foto es obligatoria",
                style = MaterialTheme.typography.labelMedium,
                color = c.error,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = onTomarFoto,
                enabled = !preparando,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = c.primaryContainer,
                    contentColor = c.onPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (rutaFoto == null) "Tomar foto" else "Otra foto")
            }
            OutlinedButton(
                onClick = onElegirGaleria,
                enabled = !preparando,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Galería")
            }
        }
    }
}

@Composable
fun BloqueNotaVoz(
    nota: String,
    dictando: Boolean,
    aviso: AvisoVoz?,
    conError: Boolean,
    limite: Int,
    onNotaChange: (String) -> Unit,
    onDictar: () -> Unit,
    onDetener: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(modifier) {
        EncabezadoBloque(Icons.Outlined.Mic, c.errorContainer, c.onErrorContainer, "Nota de voz", "Opcional")
        Spacer(Modifier.height(8.dp))
        Text(
            "Cuenta el concepto o la inspiración de la obra. Después puedes corregir el texto.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        if (dictando) {
            Button(
                onClick = onDetener,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Escuchando… toca para detener")
            }
        } else {
            OutlinedButton(
                onClick = onDictar,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (nota.isBlank()) "Dictar nota" else "Dictar y agregar")
            }
        }
        if (aviso != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.secondaryContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = c.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(aviso.texto, style = MaterialTheme.typography.bodyMedium, color = c.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (dictando) "Transcribiendo…" else "Concepto de la obra",
            style = MaterialTheme.typography.labelMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = nota,
            onValueChange = onNotaChange,
            isError = conError,
            placeholder = { Text("Escribe aquí el concepto de la obra") },
            supportingText = { Text(if (conError) "Máximo $limite caracteres" else "${nota.length}/$limite") },
            minLines = 4,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.surfaceContainerLowest,
                unfocusedContainerColor = c.surfaceContainerLowest,
                unfocusedBorderColor = c.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BloquesObraPreview() {
    TecnisisGaleriaTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BloqueFoto(rutaFoto = null, preparando = false, conError = true, onTomarFoto = {}, onElegirGaleria = {})
            BloqueNotaVoz(
                nota = "Pinté el amanecer desde el muelle de Paracas.",
                dictando = false,
                aviso = AvisoVoz.FALLO,
                conError = false,
                limite = 1000,
                onNotaChange = {},
                onDictar = {},
                onDetener = {}
            )
        }
    }
}