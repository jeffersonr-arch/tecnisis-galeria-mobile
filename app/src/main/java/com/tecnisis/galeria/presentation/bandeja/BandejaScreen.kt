package com.tecnisis.galeria.presentation.bandeja

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.presentation.componentes.ChipEstado
import com.tecnisis.galeria.presentation.componentes.TarjetaGaleria
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE"))

@Composable
fun BandejaRoute(
    onAbrir: (String) -> Unit,
    viewModel: BandejaViewModel = viewModel(factory = BandejaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    BandejaScreen(
        uiState = uiState,
        onPestanaChange = viewModel::onPestanaChange,
        onAbrir = onAbrir
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandejaScreen(
    uiState: BandejaUiState,
    onPestanaChange: (PestanaBandeja) -> Unit,
    onAbrir: (String) -> Unit
) {
    val c = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Postulaciones",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        PrimaryTabRow(
            selectedTabIndex = uiState.pestana.ordinal,
            containerColor = c.background,
            contentColor = c.primary
        ) {
            PestanaBandeja.entries.forEach { pestana ->
                Tab(
                    selected = pestana == uiState.pestana,
                    onClick = { onPestanaChange(pestana) },
                    selectedContentColor = c.primary,
                    unselectedContentColor = c.onSurfaceVariant,
                    text = {
                        Text(
                            "${pestana.titulo} (${uiState.cantidad(pestana)})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }

        when {
            uiState.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.mensajeError != null -> Text(
                uiState.mensajeError,
                color = c.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        uiState.pestana.ayuda,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                if (uiState.visibles.isEmpty()) {
                    item {
                        TarjetaGaleria {
                            Text(
                                uiState.pestana.textoVacio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.visibles, key = { it.uid }) { postulacion ->
                        TarjetaPostulante(postulacion, onClick = { onAbrir(postulacion.uid) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPostulante(p: Postulacion, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val fecha = p.fechaRegistro?.let { formatoFecha.format(Date(it)) } ?: "Sin fecha"

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = c.surfaceContainerLowest,
        border = BorderStroke(1.dp, c.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "DNI ${p.dni} · $fecha",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                ChipEstado(p.estado)
            }
            if (p.estado == EstadoPostulacion.OBSERVADA && p.motivo != null) {
                Text(
                    p.motivo,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BandejaScreenPreview() {
    val rosa = Postulacion(
        uid = "1", dni = "11223344", nombres = "Rosa Maria", apellidoPaterno = "Quispe",
        apellidoMaterno = "Mamani", direccion = "Av. Jose Olaya 133", sexo = "Femenino",
        telefono = "999888777", correo = "rosa@tecnisis.pe", estado = EstadoPostulacion.OBSERVADA,
        motivo = "Los nombres no coinciden con RENIEC (ROSA QUISPE MAMANI)",
        fechaRegistro = 1_791_000_000_000
    )
    TecnisisGaleriaTheme {
        BandejaScreen(
            uiState = BandejaUiState(
                cargando = false,
                observadas = listOf(rosa, rosa.copy(uid = "2", nombres = "Andre", apellidoPaterno = "Valenzuela")),
                verificadas = emptyList()
            ),
            onPestanaChange = {},
            onAbrir = {}
        )
    }
}