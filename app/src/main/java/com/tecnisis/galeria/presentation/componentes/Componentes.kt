package com.tecnisis.galeria.presentation.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    habilitado: Boolean = true,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
    modifier: Modifier = Modifier,
    iconoInicial: ImageVector? = null,
    prefijo: String? = null,
    accionFinal: (@Composable () -> Unit)? = null,
    ultimoCampo: Boolean = false,
    onListo: () -> Unit = {}
) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            leadingIcon = if (iconoInicial != null) { { Icon(iconoInicial, contentDescription = null) } } else null,
            prefix = if (prefijo != null) { { Text(prefijo) } } else null,
            trailingIcon = accionFinal,
            singleLine = true,
            enabled = habilitado,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = tipoTeclado,
                imeAction = if (ultimoCampo) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = { onListo() }),
            visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.surfaceContainerLowest,
                unfocusedContainerColor = c.surfaceContainerLowest,
                disabledContainerColor = c.surfaceVariant,
                unfocusedBorderColor = c.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    cargando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
            )
            if (icono != null) {
                Spacer(Modifier.width(10.dp))
                Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraConVolver(titulo: String, onVolver: () -> Unit) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleSmall) },
        navigationIcon = {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}