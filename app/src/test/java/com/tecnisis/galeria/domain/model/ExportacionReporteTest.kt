package com.tecnisis.galeria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** HU12: CSV (comillas, montos, cabecera en la primera fila) y nombre del archivo exportado. */
class ExportacionReporteTest {

    private val lima: TimeZone = TimeZone.getTimeZone("America/Lima")
    private val generado = Calendar.getInstance(lima).apply { clear(); set(2026, 10, 15, 14, 30) }.timeInMillis
    private val tasada = Calendar.getInstance(lima).apply { clear(); set(2026, 10, 14, 9, 5) }.timeInMillis

    private fun obra(titulo: String, pvp: Long, porcentaje: Int, nombre: String? = "María Elena López Díaz") =
        ObraProyectada(
            obraId = titulo, uid = "maria", titulo = titulo, tecnica = Tecnica.OLEO,
            nombreSocio = nombre, numeroSocio = if (nombre == null) null else "SOC-0002",
            pvpCentimos = pvp, porcentaje = porcentaje,
            montoGaleriaCentimos = (pvp * porcentaje + 50) / 100,
            montoSocioCentimos = pvp - (pvp * porcentaje + 50) / 100,
            fechaTasacion = tasada
        )

    @Test
    fun campos_con_coma_o_comillas_van_entre_comillas() {
        assertEquals("Boceto I", campoCsv("Boceto I"))
        assertEquals("\"Mañana, en Paracas\"", campoCsv("Mañana, en Paracas"))
        assertEquals("\"El \"\"Huaco\"\" nuevo\"", campoCsv("El \"Huaco\" nuevo"))
    }

    @Test
    fun montos_con_punto_y_sin_separador_de_miles() {
        assertEquals("1250.50", montoCsv(125_050))
        assertEquals("0.00", montoCsv(0))
        assertEquals("1000000.00", montoCsv(100_000_000))
    }

    @Test
    fun cabecera_en_la_primera_fila_y_una_fila_por_obra() {
        val csv = textoCsv(calcularProyeccion(listOf(obra("Mañana, en Paracas", 450_000, 30), obra("Boceto I", 40_000, 0, nombre = null))), lima)
        val filas = csv.split("\r\n")
        assertEquals(
            "Obra,Socio,Número de socio,Técnica,Fecha de tasación,PVP,Porcentaje,Ganancia de la galería,Socio recibe",
            filas[0]
        )
        assertEquals("\"Mañana, en Paracas\",María Elena López Díaz,SOC-0002,Óleo,14/11/2026,4500.00,30,1350.00,3150.00", filas[1])
        assertEquals("Boceto I,Sin dato,,Óleo,14/11/2026,400.00,0,0.00,400.00", filas[2])
        assertEquals("", filas[3])   // el archivo termina en \r\n
        assertEquals(4, filas.size)
    }

    @Test
    fun nombre_del_archivo_y_fecha_de_generacion_con_la_hora_del_celular() {
        assertEquals("proyeccion-20261115-1430.pdf", nombreReporte(generado, lima, FormatoReporte.PDF))
        assertEquals("proyeccion-20261115-1430.csv", nombreReporte(generado, lima, FormatoReporte.CSV))
        assertEquals("Generado el 15/11/2026 14:30", textoGenerado(generado, lima))
    }
}