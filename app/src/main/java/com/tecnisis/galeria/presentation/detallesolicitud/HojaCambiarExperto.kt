package com.tecnisis.galeria.presentation.detallesolicitud

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.domain.model.CARGA_MAXIMA
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.OpcionExperto
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.presentation.componentes.ChipColor
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaCambiarExperto(
    obra: ObraEnRevision,
    opciones: List<OpcionExperto>,
    elegidoUid: String?,
    onElegir: (String) -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        ContenidoCambiarExperto(obra, opciones, elegidoUid, onElegir, onCerrar)
    }
}

/** El contenido va aparte para poder verlo en la Preview (ModalBottomSheet no se dibuja ahí). */
@Composable
fun ContenidoCambiarExperto(
    obra: ObraEnRevision,
    opciones: List<OpcionExperto>,
    elegidoUid: String?,
    onElegir: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val disponibles = opciones.filter { it.disponible }
    val noDisponibles = opciones.filterNot { it.disponible }
    var marcado by remember(elegidoUid) {
        mutableStateOf(elegidoUid?.takeIf { uid -> disponibles.any { it.experto.uid == uid } } ?: disponibles.firstOrNull()?.experto?.uid)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("Cambiar Experto", style = MaterialTheme.typography.headlineSmall)
            Text("${obra.titulo} · ${obra.tecnica.etiqueta}", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        Surface(color = c.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Text(
                "Solo puedes elegir Expertos disponibles que dominan ${obra.tecnica.etiqueta}. Están ordenados por afinidad con la obra.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        if (disponibles.isEmpty()) {
            Text("No hay Expertos disponibles para esta técnica.", style = MaterialTheme.typography.bodyMedium, color = c.error)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            disponibles.forEach { opcion ->
                val seleccionado = opcion.experto.uid == marcado
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (seleccionado) c.primaryContainer else c.surfaceContainerLowest,
                    border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) c.primary else c.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = seleccionado, role = Role.RadioButton, onClick = { marcado = opcion.experto.uid })
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = seleccionado, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(opcion.experto.nombre, style = MaterialTheme.typography.titleSmall)
                                if (opcion.recomendado) ChipColor("Recomendado", c.secondaryContainer, c.onSecondaryContainer)
                            }
                            Text(
                                opcion.experto.tecnicas.joinToString(" · ") { it.etiqueta },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.onSurfaceVariant
                            )
                            Text(
                                "Afinidad ${opcion.afinidad} % · Carga ${opcion.experto.carga} de $CARGA_MAXIMA",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        if (noDisponibles.isNotEmpty()) {
            Column {
                TituloSeccion("No disponibles")
                noDisponibles.forEach { opcion ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(opcion.experto.nombre, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(
                            if (opcion.experto.estado == EstadoEspecialista.INACTIVO) "Inactivo" else "Ocupado · ${opcion.experto.carga} de $CARGA_MAXIMA",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = { marcado?.let(onElegir) },
                enabled = marcado != null,
                modifier = Modifier.weight(1f)
            ) { Text("Usar este Experto") }
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContenidoCambiarExpertoPreview() {
    val obra = ObraEnRevision(
        id = "1", titulo = "Mañana en Paracas", tecnica = Tecnica.OLEO, altoCm = 60.0, anchoCm = 80.0,
        profundidadCm = null, nota = "", fotoRuta = "", descripcionIa = null,
        candidatos = emptyList(), recomendadoUid = "c"
    )
    fun experto(uid: String, nombre: String, estado: EstadoEspecialista, carga: Int, vararg t: Tecnica) =
        PerfilEspecialista(uid, nombre, t.toSet(), estado, carga)
    val opciones = listOf(
        OpcionExperto(experto("c", "Carmen Villanueva", EstadoEspecialista.ACTIVO, 2, Tecnica.OLEO, Tecnica.ACRILICO), 87, true, true),
        OpcionExperto(experto("a", "Andrés Salazar Ríos", EstadoEspecialista.ACTIVO, 4, Tecnica.OLEO, Tecnica.ACUARELA), 81, false, true),
        OpcionExperto(experto("r", "Raúl Mendoza Lazo", EstadoEspecialista.ACTIVO, 5, Tecnica.OLEO), 90, false, false),
        OpcionExperto(experto("e", "Elena Torres Ugarte", EstadoEspecialista.INACTIVO, 0, Tecnica.OLEO), 70, false, false)
    )
    TecnisisGaleriaTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            ContenidoCambiarExperto(obra, opciones, elegidoUid = "c", onElegir = {}, onCerrar = {})
        }
    }
}