package com.tecnisis.galeria

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.tecnisis.galeria.domain.model.EXTRA_ABRIR_MIS_OBRAS
import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.presentation.navigation.GaleriaNavHost
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

class MainActivity : FragmentActivity() { // FragmentActivity, porque BiometricPrompt lo necesita

    private val abrirMisObras = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo la primera vez: al girar el celular no se vuelve a navegar
        if (savedInstanceState == null) leerNotificacion(intent)
        val container = (application as GaleriaApplication).container

        setContent {
            val modoTema by remember { container.observarModoTema() }
                .collectAsState(initial = ModoTema.SISTEMA)

            val oscuro = when (modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }

            // Los íconos de la barra de estado (hora, batería) siguen al tema de la app
            DisposableEffect(oscuro) {
                val estilo = if (oscuro) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }

            TecnisisGaleriaTheme(darkTheme = oscuro) {
                GaleriaNavHost(
                    abrirMisObras = abrirMisObras.value,
                    onMisObrasAbierta = { abrirMisObras.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerNotificacion(intent)
    }

    private fun leerNotificacion(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ABRIR_MIS_OBRAS, false) == true) {
            abrirMisObras.value = true
        }
    }
}