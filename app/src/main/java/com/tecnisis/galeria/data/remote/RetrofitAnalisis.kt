package com.tecnisis.galeria.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitAnalisis {

    // Misma región que resolverPostulacion. Si da 404, usa la "Function URL" que imprime el deploy (termina en /).
    private const val URL_BASE = "https://southamerica-east1-tecnisis-galeria.cloudfunctions.net/"

    private val json = Json { ignoreUnknownKeys = true }

    fun crearApi(): AnalisisApi {
        // Describir 10 obras con su foto puede pasar de 1 minuto: 90 s de lectura (la función corta a los 120 s)
        val cliente = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        return Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnalisisApi::class.java)
    }
}