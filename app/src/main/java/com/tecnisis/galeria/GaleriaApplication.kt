package com.tecnisis.galeria

import android.app.Application
import com.tecnisis.galeria.di.AppContainer
import com.tecnisis.galeria.di.DefaultAppContainer

class GaleriaApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}