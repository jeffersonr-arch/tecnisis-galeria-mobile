package com.tecnisis.galeria

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.tecnisis.galeria.presentation.navigation.GaleriaNavHost
import com.tecnisis.galeria.presentation.theme.TecnisisGaleriaTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TecnisisGaleriaTheme {
                GaleriaNavHost()
            }
        }
    }
}