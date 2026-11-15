package com.tecnisis.galeria.presentation.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.DashboardSolicitudes
import com.tecnisis.galeria.domain.model.ExpertoDashboard
import com.tecnisis.galeria.domain.model.FilaRanking
import com.tecnisis.galeria.domain.model.FiltroFecha
import com.tecnisis.galeria.domain.model.FiltrosDashboard
import com.tecnisis.galeria.domain.model.ResumenDona
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.model.hoyEnUtc
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import com.tecnisis.galeria.presentation.theme.galeria
import java.util.TimeZone

@Composable
fun DashboardSolicitudesRoute(
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardSolicitudesScreen(
        uiState = uiState,
        onFecha = viewModel::onFecha,
        onTecnica = viewModel::onTecnica,
        onSocio = viewModel::onSocio,
        onExperto = viewModel::onExperto,
        onAbrirRango = viewModel::onAbrirRango,
        onCerrarRango = viewModel::onCerrarRango,
        onRangoElegido = viewModel::onRangoElegido
    )
}

@Composable
fun DashboardSolicitudesScreen(
    uiState: DashboardUiState,
    onFecha: (FiltroFecha) -> Unit,
    onTecnica: (Tecnica?) -> Unit,
    onSocio: (String?) -> Unit,
    onExperto: (String?) -> Unit,
    onAbrirRango: () -> Unit,
    onCerrarRango: () -> Unit,
    onRangoElegido: (Long, Long) -> Unit
) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        val dashboard = uiState.dashboard
        when {
            uiState.cargando ->
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Encabezado()
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

            uiState.mensajeError != null || dashboard == null ->
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Encabezado()
                    EstadoError(uiState.mensajeError ?: "No se pudo cargar el dashboard. Inténtalo de nuevo")
                }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Encabezado(Modifier.padding(horizontal = 8.dp)) }
                item { ChipsFecha(uiState.filtros.fecha, uiState.textoChipRango, onFecha, onAbrirRango) }
                item { Filtros(uiState, dashboard, onTecnica, onSocio, onExperto) }
                item { TarjetaDona(dashboard.dona) }
                item { TarjetaRanking(dashboard.ranking) }
            }
        }
        if (uiState.eligiendoRango) {
            DialogoRango(onCerrar = onCerrarRango, onGuardar = onRangoElegido)
        }
    }
}

@Composable
private fun Encabezado(modifier: Modifier = Modifier) {
    Column(modifier) {
        Spacer(Modifier.height(24.dp))
        Text("Solicitudes", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Obras evaluadas por los Expertos · en tiempo real",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstadoError(mensaje: String) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = c.outline, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No se pudo cargar el dashboard", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** 7 días · 30 días · Todo · Rango… */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsFecha(
    fecha: FiltroFecha,
    textoRango: String,
    onFecha: (FiltroFecha) -> Unit,
    onAbrirRango: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipFecha("7 días", fecha == FiltroFecha.SieteDias) { onFecha(FiltroFecha.SieteDias) }
        ChipFecha("30 días", fecha == FiltroFecha.TreintaDias) { onFecha(FiltroFecha.TreintaDias) }
        ChipFecha("Todo", fecha == FiltroFecha.Todo) { onFecha(FiltroFecha.Todo) }
        ChipFecha(textoRango, fecha is FiltroFecha.Rango, onAbrirRango)
    }
}

@Composable
private fun ChipFecha(texto: String, elegido: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    FilterChip(
        selected = elegido,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (elegido) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = c.primaryContainer,
            selectedLabelColor = c.onPrimaryContainer,
            selectedLeadingIconColor = c.onPrimaryContainer
        )
    )
}

/** Los tres desplegables; cada uno empieza en "Todos" y se combinan entre sí. */
@Composable
private fun Filtros(
    uiState: DashboardUiState,
    dashboard: DashboardSolicitudes,
    onTecnica: (Tecnica?) -> Unit,
    onSocio: (String?) -> Unit,
    onExperto: (String?) -> Unit
) {
    val todos = DashboardUiState.TODOS
    val opcionesTecnica: List<Pair<Tecnica?, String>> =
        listOf<Pair<Tecnica?, String>>(null to todos) + Tecnica.entries.map { it to it.etiqueta }
    val opcionesSocio: List<Pair<String?, String>> =
        listOf<Pair<String?, String>>(null to todos) + dashboard.socios.map { it to it }
    val opcionesExperto: List<Pair<String?, String>> =
        listOf<Pair<String?, String>>(null to todos) + dashboard.expertos.map { it.uid to etiquetaExperto(it) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Desplegable("Técnica", uiState.textoTecnica, uiState.filtros.tecnica != null, opcionesTecnica, onTecnica)
        Desplegable("Socio", uiState.textoSocio, uiState.filtros.socio != null, opcionesSocio, onSocio)
        Desplegable("Experto", uiState.textoExperto, uiState.filtros.expertoUid != null, opcionesExperto, onExperto)
    }
}

/** Campo de solo lectura que abre un menú con las opciones. */
@Composable
private fun <T> Desplegable(
    etiqueta: String,
    valor: String,
    activo: Boolean,
    opciones: List<Pair<T?, String>>,
    onElegir: (T?) -> Unit
) {
    val c = MaterialTheme.colorScheme
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (activo) c.primary else c.outline,
                unfocusedLabelColor = if (activo) c.primary else c.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        // Capa encima del campo: el toque abre el menú (el campo no se escribe)
        Box(
            Modifier
                .matchParentSize()
                .clickable { abierto = true }
                .semantics {
                    contentDescription = "$etiqueta: $valor"
                    role = Role.DropdownList
                }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (clave, texto) ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        abierto = false
                        onElegir(clave)
                    }
                )
            }
        }
    }
}

@Composable
private fun TarjetaDona(dona: ResumenDona) {
    val c = MaterialTheme.colorScheme
    val g = MaterialTheme.galeria
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Text("Aceptadas y rechazadas", style = MaterialTheme.typography.titleLarge)
        Text(
            "Veredictos de los Expertos con estos filtros",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        if (dona.total == 0) {
            SinObras()
        } else {
            Box(
                Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                GraficoDona(
                    aceptadas = dona.aceptadas,
                    rechazadas = dona.rechazadas,
                    descripcion = "Aceptadas ${dona.aceptadas}, ${dona.porcentajeAceptadas} %; " +
                            "rechazadas ${dona.rechazadas}, ${dona.porcentajeRechazadas} %",
                    modifier = Modifier.fillMaxSize()
                )
                Text(dona.textoCentro, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            FilaLeyenda("Aceptadas", "${dona.aceptadas} · ${dona.porcentajeAceptadas} %", c.tertiary)
            Spacer(Modifier.height(8.dp))
            FilaLeyenda("Rechazadas", "${dona.rechazadas} · ${dona.porcentajeRechazadas} %", g.pigmentoRojo)
        }
    }
}

@Composable
private fun FilaLeyenda(rotulo: String, valor: String, color: Color) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(rotulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SinObras() {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.DonutLarge, contentDescription = null, tint = c.outline, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            "No hay obras evaluadas con estos filtros",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Text(
            "Prueba con otro rango de fechas o vuelve a «Todos».",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaRanking(ranking: List<FilaRanking>) {
    val c = MaterialTheme.colorScheme
    TarjetaGaleria(relleno = PaddingValues(16.dp)) {
        Text("Ranking de Expertos", style = MaterialTheme.typography.titleLarge)
        Text(
            "Evaluadas con estos filtros · pendientes ahora",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        if (ranking.isEmpty()) {
            Text("Todavía no hay Expertos registrados", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        val maximo = ranking.maxOfOrNull { it.evaluadas } ?: 0
        ranking.forEachIndexed { indice, fila ->
            if (indice > 0) Spacer(Modifier.height(8.dp))
            FilaRankingExperto(puesto = indice + 1, fila = fila, maximo = maximo)
        }
    }
}

@Composable
private fun FilaRankingExperto(puesto: Int, fila: FilaRanking, maximo: Int) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(c.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("$puesto", style = MaterialTheme.typography.labelLarge, color = c.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fila.nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                if (!fila.activo) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Inactivo",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.outlineVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.outlineVariant)
            ) {
                if (maximo > 0 && fila.evaluadas > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(fila.evaluadas.toFloat() / maximo)
                            .fillMaxHeight()
                            .background(c.primary)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Pendientes ahora: ${fila.pendientes}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${fila.evaluadas}", style = MaterialTheme.typography.headlineSmall)
            Text("evaluadas", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}

/** DateRangePicker de Material 3 en pantalla completa; los días futuros no se pueden elegir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRango(onCerrar: () -> Unit, onGuardar: (Long, Long) -> Unit) {
    val hoy = remember { hoyEnUtc(System.currentTimeMillis(), TimeZone.getDefault()) }
    val estado = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= hoy
        }
    )
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cancelar") }
                    Spacer(Modifier.weight(1f))
                    val inicio = estado.selectedStartDateMillis
                    val fin = estado.selectedEndDateMillis
                    TextButton(
                        enabled = inicio != null && fin != null,
                        onClick = { if (inicio != null && fin != null) onGuardar(inicio, fin) }
                    ) { Text("Guardar") }
                }
                DateRangePicker(
                    state = estado,
                    modifier = Modifier.weight(1f),
                    title = {
                        Text(
                            "Selecciona el rango",
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 8.dp)
                        )
                    },
                    showModeToggle = false
                )
            }
        }
    }
}

// --- Vistas previas ---

private val expertosEjemplo = listOf(
    ExpertoDashboard("a2", "Andrés Salazar Ríos", activo = true, carga = 4),
    ExpertoDashboard("a6", "Elena Torres Ugarte", activo = false, carga = 0),
    ExpertoDashboard("a4", "Jorge Huamán Ccori", activo = true, carga = 1),
    ExpertoDashboard("a5", "Lucía Paredes Rojas", activo = true, carga = 0),
    ExpertoDashboard("a3", "Patricia Núñez Vega", activo = true, carga = 0)
)

private val dashboardEjemplo = DashboardSolicitudes(
    dona = ResumenDona(aceptadas = 12, rechazadas = 5),
    ranking = listOf(
        FilaRanking("a2", "Andrés Salazar Ríos", true, evaluadas = 6, pendientes = 4),
        FilaRanking("a3", "Patricia Núñez Vega", true, evaluadas = 5, pendientes = 0),
        FilaRanking("a4", "Jorge Huamán Ccori", true, evaluadas = 4, pendientes = 1),
        FilaRanking("a5", "Lucía Paredes Rojas", true, evaluadas = 2, pendientes = 0),
        FilaRanking("a6", "Elena Torres Ugarte", false, evaluadas = 0, pendientes = 0)
    ),
    socios = listOf("María Elena López Díaz · SOC-0002", "Rosa Quispe Mamani · SOC-0001"),
    expertos = expertosEjemplo
)

@Composable
private fun VistaPrevia(uiState: DashboardUiState) {
    TecnisisGaleriaTheme {
        DashboardSolicitudesScreen(uiState, {}, {}, {}, {}, {}, {}, { _, _ -> })
    }
}

@Preview(name = "Todo · claro", showBackground = true, heightDp = 1500)
@Preview(name = "Todo · nocturno", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardTodoPreview() =
    VistaPrevia(DashboardUiState(cargando = false, dashboard = dashboardEjemplo))

@Preview(name = "Filtros combinados", showBackground = true, heightDp = 1100)
@Composable
private fun DashboardFiltradoPreview() = VistaPrevia(
    DashboardUiState(
        cargando = false,
        filtros = FiltrosDashboard(fecha = FiltroFecha.TreintaDias, tecnica = Tecnica.OLEO, expertoUid = "a2"),
        dashboard = dashboardEjemplo.copy(
            dona = ResumenDona(aceptadas = 1, rechazadas = 2),
            ranking = listOf(FilaRanking("a2", "Andrés Salazar Ríos", true, evaluadas = 3, pendientes = 4))
        )
    )
)

@Preview(name = "Sin obras · claro", showBackground = true, heightDp = 1300)
@Preview(name = "Sin obras · nocturno", showBackground = true, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardVacioPreview() = VistaPrevia(
    DashboardUiState(
        cargando = false,
        filtros = FiltrosDashboard(fecha = FiltroFecha.SieteDias),
        dashboard = dashboardEjemplo.copy(
            dona = ResumenDona(0, 0),
            ranking = dashboardEjemplo.ranking.map { it.copy(evaluadas = 0) }
        )
    )
)

@Preview(name = "Error · claro", showBackground = true)
@Preview(name = "Error · nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardErrorPreview() = VistaPrevia(
    DashboardUiState(cargando = false, mensajeError = "Sin conexión. Revisa tu internet y vuelve a entrar.")
)