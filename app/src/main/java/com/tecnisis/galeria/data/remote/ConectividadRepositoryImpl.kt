package com.tecnisis.galeria.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.tecnisis.galeria.domain.repository.ConectividadRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ConectividadRepositoryImpl(contexto: Context) : ConectividadRepository {

    private val conectividad = contexto.getSystemService(ConnectivityManager::class.java)

    override fun observarConexion(): Flow<Boolean> = callbackFlow {
        // Android avisa cuando la red por defecto aparece o se pierde
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(hayConexionAhora()) // valor inicial, antes del primer aviso
        conectividad.registerDefaultNetworkCallback(escucha)
        awaitClose { conectividad.unregisterNetworkCallback(escucha) }
    }.distinctUntilChanged()

    private fun hayConexionAhora(): Boolean {
        val red = conectividad.activeNetwork ?: return false
        val capacidades = conectividad.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}