package com.tecnisis.galeria.presentation.obras

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.domain.model.CampoObra
import com.tecnisis.galeria.domain.model.NOTA_MAXIMA
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.componentes.BarraConVolver
import com.tecnisis.galeria.presentation.componentes.BotonPrincipal
import com.tecnisis.galeria.presentation.componentes.CampoTexto
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

private const val MENSAJE_MEDIDA = "Número mayor que 0 y hasta 1000"

@Composable
fun RegistrarObraScreen(
    uiState: RegistrarObraUiState,
    snackbarHostState: SnackbarHostState,
    acciones: AccionesRegistrarObra
) {
    val c = MaterialTheme.colorScheme
    Scaffold(
        containerColor = c.background,
        topBar = { BarraConVolver(if (uiState.esEdicion) "Editar obra" else "Registrar obra", acciones.onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                BotonPrincipal(
                    texto = if (uiState.esEdicion) "Guardar cambios" else "Guardar obra",
                    cargando = uiState.guardando,
                    onClick = acciones.onGuardar,
                    icono = Icons.Filled.Check
                )
            }
        }
    ) { relleno ->
        if (uiState.cargando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(relleno),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Formulario(uiState, acciones, Modifier.padding(relleno))
        }
    }
    if (uiState.confirmandoEliminar) {
        DialogoEliminar(uiState.titulo, acciones.onConfirmarEliminar, acciones.onCancelarEliminar)
    }
}

@Composable
private fun Formulario(uiState: RegistrarObraUiState, acciones: AccionesRegistrarObra, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.padding(start = 4.dp, top = 4.dp)) {
            Text(if (uiState.esEdicion) "Editar obra" else "Nueva obra", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Se guarda en tu celular aunque no tengas internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        }
        BloqueFoto(
            rutaFoto = uiState.fotoLocal,
            preparando = uiState.preparandoFoto,
            conError = uiState.tieneError(CampoObra.FOTO),
            onTomarFoto = acciones.onTomarFoto,
            onElegirGaleria = acciones.onElegirGaleria
        )
        BloqueDatos(uiState, acciones)
        BloqueDimensiones(uiState, acciones)
        BloqueNotaVoz(
            nota = uiState.nota,
            dictando = uiState.dictando,
            aviso = uiState.avisoVoz,
            conError = uiState.tieneError(CampoObra.NOTA),
            limite = NOTA_MAXIMA,
            onNotaChange = acciones.onNotaChange,
            onDictar = acciones.onDictar,
            onDetener = acciones.onDetenerDictado
        )
        if (uiState.esEdicion) {
            TextButton(onClick = acciones.onPedirEliminar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Eliminar obra", color = c.error)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueDatos(uiState: RegistrarObraUiState, acciones: AccionesRegistrarObra) {
    val c = MaterialTheme.colorScheme
    val errorTecnica = uiState.tieneError(CampoObra.TECNICA)
    TarjetaGaleria {
        EncabezadoBloque(Icons.Outlined.Palette, c.secondaryContainer, c.onSecondaryContainer, "Datos de la obra")
        Spacer(Modifier.height(16.dp))
        CampoTexto(
            valor = uiState.titulo,
            onCambio = acciones.onTituloChange,
            etiqueta = "Título",
            error = if (uiState.tieneError(CampoObra.TITULO)) "Escribe el título de la obra" else null
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Técnica",
            style = MaterialTheme.typography.labelMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tecnica.entries.forEach { tecnica ->
                val elegida = uiState.tecnica == tecnica
                FilterChip(
                    selected = elegida,
                    onClick = { acciones.onTecnicaChange(tecnica) },
                    label = { Text(tecnica.etiqueta) },
                    leadingIcon = if (elegida) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.primaryContainer,
                        selectedLabelColor = c.onPrimaryContainer,
                        selectedLeadingIconColor = c.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = elegida,
                        borderColor = if (errorTecnica) c.error else c.outlineVariant
                    )
                )
            }
        }
        if (errorTecnica) {
            Text(
                "Elige una técnica",
                style = MaterialTheme.typography.labelMedium,
                color = c.error,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun BloqueDimensiones(uiState: RegistrarObraUiState, acciones: AccionesRegistrarObra) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria {
        EncabezadoBloque(Icons.Outlined.Straighten, c.tertiaryContainer, c.onTertiaryContainer, "Dimensiones", "en cm")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoTexto(
                valor = uiState.alto,
                onCambio = acciones.onAltoChange,
                etiqueta = "Alto",
                error = if (uiState.tieneError(CampoObra.ALTO)) MENSAJE_MEDIDA else null,
                tipoTeclado = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            CampoTexto(
                valor = uiState.ancho,
                onCambio = acciones.onAnchoChange,
                etiqueta = "Ancho",
                error = if (uiState.tieneError(CampoObra.ANCHO)) MENSAJE_MEDIDA else null,
                tipoTeclado = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        CampoTexto(
            valor = uiState.profundidad,
            onCambio = acciones.onProfundidadChange,
            etiqueta = "Profundidad (opcional)",
            error = if (uiState.tieneError(CampoObra.PROFUNDIDAD)) MENSAJE_MEDIDA else null,
            tipoTeclado = KeyboardType.Decimal,
            ultimoCampo = true
        )
        Text(
            "Solo si la obra tiene volumen (escultura, cerámica).",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )
    }
}

@Composable
private fun DialogoEliminar(titulo: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = c.surfaceContainerLowest,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.error) },
        title = { Text("¿Eliminar esta obra?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "“${titulo.ifBlank { "Sin título" }}” se borrará de tu solicitud y de tu celular. No se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Eliminar", color = c.error) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

// ---------- Previews ----------

private val obraCompleta = RegistrarObraUiState(
    titulo = "Mañana en Paracas",
    tecnica = Tecnica.OLEO,
    alto = "60",
    ancho = "80",
    fotoLocal = "ejemplo",   // en la Preview no existe: se ven las manchas de pigmento
    nota = "Pinté el amanecer desde el muelle de Paracas: el azul del mar contra el amarillo del desierto."
)

@Composable
private fun VistaPrevia(uiState: RegistrarObraUiState) {
    TecnisisGaleriaTheme {
        RegistrarObraScreen(uiState, remember { SnackbarHostState() }, AccionesRegistrarObra())
    }
}

@Preview(name = "Completa · claro", showBackground = true, heightDp = 1500)
@Preview(name = "Completa · nocturno", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrarObraCompletaPreview() = VistaPrevia(obraCompleta)

@Preview(name = "Errores y voz fallida", showBackground = true, heightDp = 1500)
@Composable
private fun RegistrarObraErroresPreview() = VistaPrevia(
    RegistrarObraUiState(
        alto = "0",
        errores = setOf(CampoObra.TITULO, CampoObra.TECNICA, CampoObra.ALTO, CampoObra.ANCHO, CampoObra.FOTO),
        avisoVoz = AvisoVoz.FALLO
    )
)

@Preview(name = "Dictando", showBackground = true, heightDp = 1500)
@Composable
private fun RegistrarObraDictandoPreview() = VistaPrevia(
    obraCompleta.copy(dictando = true, nota = "Pinté el amanecer desde el muelle")
)

@Preview(name = "Edición · eliminar", showBackground = true, heightDp = 900)
@Preview(name = "Edición · eliminar · nocturno", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrarObraEdicionPreview() = VistaPrevia(obraCompleta.copy(esEdicion = true, confirmandoEliminar = true))