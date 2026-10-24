package com.tecnisis.galeria.domain.repository

import kotlinx.coroutines.flow.Flow

/** HU05: avisa si el celular tiene internet, para mostrar "Sin conexión" en el Perfil. */
fun interface ConectividadRepository {
    fun observarConexion(): Flow<Boolean>
}
