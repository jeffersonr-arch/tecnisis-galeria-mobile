package com.tecnisis.galeria.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DniApi {
    @GET("v1/reniec/dni")
    suspend fun consultar(
        @Query("numero") dni: String,
        @Header("Authorization") autorizacion: String
    ): Response<RespuestaDniDto>
}

/** Solo los campos que usamos; el resto del JSON se ignora. */
@Serializable
data class RespuestaDniDto(
    @SerialName("first_name") val nombres: String? = null,
    @SerialName("first_last_name") val apellidoPaterno: String? = null,
    @SerialName("second_last_name") val apellidoMaterno: String? = null
)