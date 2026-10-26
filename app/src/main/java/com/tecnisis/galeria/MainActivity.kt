package com.tecnisis.galeria

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.presentation.navigation.GaleriaNavHost
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

class MainActivity : FragmentActivity() { // FragmentActivity, porque BiometricPrompt lo necesita
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                GaleriaNavHost()
            }
        }
    }
}