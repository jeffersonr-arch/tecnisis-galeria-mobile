package com.tecnisis.galeria.presentation.inicio

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.presentation.componentes.ChipEstado
import com.tecnisis.galeria.presentation.componentes.EncabezadoGaleria
import com.tecnisis.galeria.presentation.componentes.TarjetaFuncion
import com.tecnisis.galeria.presentation.componentes.TituloSeccion
import com.tecnisis.galeria.presentation.estado.EstadoPostulacionUi
import com.tecnisis.galeria.presentation.estado.EstadoPostulacionViewModel
import com.tecnisis.galeria.presentation.estado.pasosCompletos
import com.tecnisis.galeria.presentation.estado.resumen
import com.tecnisis.galeria.presentation.navigation.Pantalla
import com.tecnisis.galeria.presentation.navigation.funciones
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

@Composable
fun InicioRolRoute(
    rol: Rol,
    onSesionCerrada: () -> Unit,
    onNavegar: (Pantalla) -> Unit = {},
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(rol) { viewModel.verificarAcceso(rol) }

    when (val e = estado) {
        EstadoInicio.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        EstadoInicio.Denegado -> LaunchedEffect(Unit) {
            viewModel.onCerrarSesion()
            onSesionCerrada()
        }
        is EstadoInicio.Autorizado -> {
            val postulacion: Postulacion? = if (rol == Rol.POSTULANTE) {
                val estadoViewModel: EstadoPostulacionViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = EstadoPostulacionViewModel.Factory)
                val estadoPostulacion by estadoViewModel.estado.collectAsState()
                (estadoPostulacion as? EstadoPostulacionUi.ConPostulacion)?.postulacion
            } else null

            InicioRolScreen(usuario = e.usuario, postulacion = postulacion, onNavegar = onNavegar)
        }
    }
}

@Composable
fun InicioRolScreen(usuario: Usuario, postulacion: Postulacion?, onNavegar: (Pantalla) -> Unit) {
    val c = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box {
            EncabezadoGaleria(
                etiquetaRol = usuario.rol.etiqueta,
                saludo = "Hola, ${usuario.nombre.substringBefore(" ")}",
                detalle = usuario.correo,
                espacioInferior = if (postulacion != null) 84.dp else 32.dp
            )
            if (postulacion != null) {
                TarjetaPostulacion(
                    postulacion = postulacion,
                    onClick = { onNavegar(Pantalla.EstadoPostulacion) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp)
                        .offset(y = 58.dp)
                )
            }
        }

        Spacer(Modifier.height(if (postulacion != null) 82.dp else 24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TituloSeccion("Tus funciones")

            val coloresIcono = listOf(
                c.primaryContainer to c.onPrimaryContainer,
                c.secondaryContainer to c.onSecondaryContainer,
                c.tertiaryContainer to c.onTertiaryContainer
            )
            usuario.rol.funciones().forEachIndexed { i, funcion ->
                val (fondo, color) = coloresIcono[i % coloresIcono.size]
                TarjetaFuncion(
                    titulo = funcion.titulo,
                    subtitulo = funcion.subtitulo,
                    icono = funcion.icono,
                    fondoIcono = fondo,
                    colorIcono = color,
                    onClick = funcion.destino?.let { destino -> { onNavegar(destino) } }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaPostulacion(postulacion: Postulacion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val completos = postulacion.estado.pasosCompletos()

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = c.surfaceContainerLowest,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, c.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tu postulación",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                ChipEstado(postulacion.estado)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i < completos) c.primary else c.surfaceVariant)
                    )
                }
            }
            Text(postulacion.estado.resumen(), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Nocturno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InicioRolScreenPreview() {
    TecnisisGaleriaTheme {
        InicioRolScreen(
            usuario = Usuario("1", "Pedro Castillo", "pedro.castillo@correo.com", Rol.POSTULANTE),
            postulacion = Postulacion(
                uid = "1", dni = "88776655", nombres = "Pedro", apellidoPaterno = "Castillo",
                apellidoMaterno = "Huaman", direccion = "Av. Lima 123", sexo = "Masculino",
                telefono = "987654321", correo = "pedro.castillo@correo.com",
                estado = EstadoPostulacion.VERIFICADA
            ),
            onNavegar = {}
        )
    }
}