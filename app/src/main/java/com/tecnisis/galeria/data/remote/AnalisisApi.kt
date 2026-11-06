package com.tecnisis.galeria.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** HU07: la función HTTP analizarSolicitud (Gemini se llama desde ahí, nunca desde la app). */
interface AnalisisApi {
    @POST("analizarSolicitud")
    suspend fun analizar(
        @Header("Authorization") autorizacion: String, // "Bearer <token de sesión>"
        @Body pedido: PedidoAnalisisDto
    ): Response<RespuestaAnalisisDto>
}

@Serializable
data class PedidoAnalisisDto(val solicitudId: String)

/** 200 Listo · 202 en curso (otra llamada lo está haciendo) · 4xx/5xx con "error". */
@Serializable
data class RespuestaAnalisisDto(
    val analisis: String? = null,
    val obrasSinExperto: Int? = null,
    val error: String? = null
)