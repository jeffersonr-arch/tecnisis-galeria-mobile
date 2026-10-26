package com.tecnisis.galeria.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecnisis.galeria.domain.model.ModoTema
import com.tecnisis.galeria.domain.repository.PreferenciasRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Un solo DataStore para toda la app: se declara una vez, fuera de la clase
private val Context.preferencias by preferencesDataStore(name = "preferencias")

class PreferenciasRepositoryImpl(private val contexto: Context) : PreferenciasRepository {

    override fun observarModoTema(): Flow<ModoTema> =
        contexto.preferencias.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { prefs ->
                ModoTema.entries.firstOrNull { it.name == prefs[CLAVE_MODO_TEMA] } ?: ModoTema.SISTEMA
            }

    override suspend fun guardarModoTema(modo: ModoTema) {
        contexto.preferencias.edit { it[CLAVE_MODO_TEMA] = modo.name }
    }

    private companion object {
        val CLAVE_MODO_TEMA = stringPreferencesKey("modo_tema")
    }
}