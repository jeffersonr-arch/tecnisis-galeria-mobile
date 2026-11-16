package com.tecnisis.galeria.domain.repository

import com.tecnisis.galeria.domain.model.ArchivoReporte
import com.tecnisis.galeria.domain.model.FormatoReporte
import com.tecnisis.galeria.domain.model.ProyeccionEconomica

/** HU12: escribe el reporte económico (PDF o CSV) en la caché del celular para compartirlo. */
interface ReporteRepository {
    /** [generado]: la hora del celular al tocar Exportar; va en el nombre del archivo y en el PDF. */
    suspend fun generar(proyeccion: ProyeccionEconomica, formato: FormatoReporte, generado: Long): Result<ArchivoReporte>
}