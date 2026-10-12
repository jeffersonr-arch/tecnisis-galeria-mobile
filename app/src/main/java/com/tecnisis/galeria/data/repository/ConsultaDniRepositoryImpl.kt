package com.tecnisis.galeria.data.repository

import com.tecnisis.galeria.data.remote.DniApi
import com.tecnisis.galeria.domain.model.PersonaReniec
import com.tecnisis.galeria.domain.repository.ConsultaDniRepository
import java.io.IOException

class ConsultaDniRepositoryImpl(
    private val api: DniApi,
    private val token: String
) : ConsultaDniRepository {

    override suspend fun consultar(dni: String): PersonaReniec? {
        // Si no hay internet, Retrofit lanza IOException y la dejamos pasar: el Worker reintenta
        val respuesta = api.consultar(dni, "Bearer $token")
        return when {
            respuesta.isSuccessful -> respuesta.body()
                ?.let {
                    PersonaReniec(
                        nombres = it.nombres.orEmpty(),
                        apellidoPaterno = it.apellidoPaterno.orEmpty(),
                        apellidoMaterno = it.apellidoMaterno.orEmpty()
                    )
                }
                ?.takeIf { it.nombres.isNotBlank() }
            respuesta.code() in listOf(400, 404, 422) -> null // DNI no encontrado o inválido
            respuesta.code() == 429 || respuesta.code() >= 500 ->
                throw IOException("Servicio de RENIEC no disponible (${respuesta.code()})")
            else -> throw IllegalStateException("La API de DNI respondió ${respuesta.code()}")
        }
    }
}