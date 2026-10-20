package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tecnisis.galeria.R
import com.tecnisis.galeria.presentation.theme.galeria

@Composable
fun LogoGaleria(tamano: Dp, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.28f))
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_busto),
            contentDescription = null,
            tint = color,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EncabezadoGaleria(
    etiquetaRol: String,
    saludo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    espacioInferior: Dp = 84.dp
) {
    val g = MaterialTheme.galeria
    val c = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(g.encabezado)
    ) {
        // Manchas de pigmento (solo decoración)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 104.dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(g.pigmentoAmarillo)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-96).dp, y = (-46).dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(g.pigmentoRojo)
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = espacioInferior)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoGaleria(40.dp, fondo = g.sobreEncabezado, color = g.encabezado)
                Spacer(Modifier.width(10.dp))
                Text("TECNISIS", style = MaterialTheme.typography.titleLarge, color = g.sobreEncabezado)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                etiquetaRol.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.onSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.secondary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(saludo, style = MaterialTheme.typography.headlineLarge, color = g.sobreEncabezado)
            Spacer(Modifier.height(4.dp))
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = g.sobreEncabezadoSuave)
        }
    }
}