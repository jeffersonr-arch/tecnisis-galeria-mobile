package com.tecnisis.galeria.data.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.tecnisis.galeria.domain.model.CopiaDocumento
import com.tecnisis.galeria.domain.model.DocumentoRechazoObra
import com.tecnisis.galeria.domain.model.DocumentoSolicitud
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.TipoDecision
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU07: dibuja el documento en una hoja A4 (595 × 842 puntos) con PdfDocument, la API de Android.
 * Las dos copias salen de aquí con el mismo contenido; solo cambia el texto de la franja de arriba.
 * Los colores son los de la paleta Pigmentos, escritos aquí porque un PDF no usa el tema de la app.
 */
class GeneradorPdf {

    private val locale = Locale.forLanguageTag("es-PE")
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", locale)
    private val formatoHora = SimpleDateFormat("h:mm a", locale)

    /** Escribe el PDF en [destino]. Si algo falla lanza la excepción (el repositorio la convierte en error). */
    fun generar(documento: DocumentoSolicitud, copia: CopiaDocumento, destino: File) {
        val pdf = PdfDocument()
        try {
            val pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
            dibujar(pagina.canvas, documento, copia)
            pdf.finishPage(pagina)
            destino.parentFile?.mkdirs()
            FileOutputStream(destino).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    private fun dibujar(canvas: Canvas, d: DocumentoSolicitud, copia: CopiaDocumento) {
        val esRechazo = d.tipo == TipoDecision.RECHAZAR
        val fecha = Date(d.fecha)

        // 1) Franja: cobalto en la aprobación, rojo en el rechazo
        canvas.drawRect(0f, 0f, ANCHO.toFloat(), 30f, relleno(if (esRechazo) ROJO else COBALTO))
        val franja = texto(8.5f, BLANCO, negrita = true).apply { letterSpacing = 0.14f; textAlign = Paint.Align.CENTER }
        canvas.drawText(copia.etiqueta.uppercase(locale), ANCHO / 2f, 19f, franja)

        // 2) Encabezado: logo, nombre de la galería, número y fecha
        dibujarLogo(canvas, MARGEN, 46f, 34f)
        canvas.drawText("TECNISIS Galería", MARGEN + 44f, 62f, texto(14f, TINTA, negrita = true, serif = true))
        canvas.drawText("Exposiciones de arte y cultura peruana", MARGEN + 44f, 76f, texto(9f, SUAVE))
        val derecha = texto(9f, SUAVE).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Solicitud N.° ${d.numero}", DERECHA, 60f, derecha)
        canvas.drawText("Fecha: ${formatoFecha.format(fecha)} · ${formatoHora.format(fecha)}", DERECHA, 74f, derecha)

        // 3) Título e introducción
        canvas.drawLine(MARGEN, 100f, DERECHA, 100f, linea(TINTA, 2f))
        var y = 112f
        y += parrafo(canvas, d.titulo, MARGEN, y, ANCHO_UTIL, texto(17f, TINTA, negrita = true, serif = true)) + 6f
        y += parrafo(canvas, d.introduccion, MARGEN, y, ANCHO_UTIL, texto(10f, TEXTO)) + 16f

        // 4) Datos del socio en un recuadro
        canvas.drawRoundRect(RectF(MARGEN, y, DERECHA, y + 76f), 8f, 8f, relleno(PAPEL))
        dato(canvas, "Socio", d.nombreSocio, MARGEN + 16f, y + 20f)
        dato(canvas, "N.° de socio", d.numeroSocio, MARGEN + 270f, y + 20f)
        dato(canvas, "DNI", d.dni, MARGEN + 16f, y + 52f)
        dato(canvas, "Correo", d.correo, MARGEN + 270f, y + 52f)
        y += 96f

        // 5) Tabla de obras
        val columnas = floatArrayOf(MARGEN, MARGEN + 22f, MARGEN + 222f, MARGEN + 312f, MARGEN + 402f)
        val encabezados = listOf("#", "Obra", "Técnica", "Medidas (cm)", d.columnaFinal)
        val cabecera = texto(9f, SUAVE, negrita = true)
        encabezados.forEachIndexed { i, titulo -> canvas.drawText(titulo, columnas[i], y, cabecera) }
        y += 7f
        canvas.drawLine(MARGEN, y, DERECHA, y, linea(TINTA, 1.5f))
        val celda = texto(10f, TINTA)
        val celdaNegrita = texto(10f, TINTA, negrita = true)
        d.filas.forEach { fila ->
            y += 18f
            val valores = listOf(fila.numero.toString(), fila.obra, fila.tecnica, fila.medidas, fila.ultima)
            valores.forEachIndexed { i, valor ->
                val ancho = (if (i < columnas.size - 1) columnas[i + 1] else DERECHA) - columnas[i] - 8f
                val pintura = if (i == 1) celdaNegrita else celda
                canvas.drawText(recortar(valor, pintura, ancho), columnas[i], y, pintura)
            }
            y += 9f
            canvas.drawLine(MARGEN, y, DERECHA, y, linea(LINEA, 1f))
        }

        // 6) Motivo del rechazo, en un recuadro rojo
        if (esRechazo && !d.motivo.isNullOrBlank()) {
            y += 20f
            val cuerpo = StaticLayout.Builder
                .obtain(d.motivo, 0, d.motivo.length, texto(10.5f, TINTA), (ANCHO_UTIL - 32f).toInt())
                .setLineSpacing(0f, 1.25f)
                .build()
            val alto = 34f + cuerpo.height
            canvas.drawRoundRect(RectF(MARGEN, y, DERECHA, y + alto), 8f, 8f, linea(ROJO, 1.5f))
            val etiqueta = texto(9f, ROJO, negrita = true).apply { letterSpacing = 0.08f }
            canvas.drawText("MOTIVO DEL RECHAZO", MARGEN + 16f, y + 20f, etiqueta)
            canvas.save()
            canvas.translate(MARGEN + 16f, y + 26f)
            cuerpo.draw(canvas)
            canvas.restore()
        }

        // 7) Quién resolvió y firma
        val pie = texto(9.5f, TEXTO)
        canvas.drawText("Resuelto por: Anfitrión · ${d.resueltoPor}", MARGEN, 752f, pie)
        canvas.drawText(
            "Decisión registrada en TECNISIS Mobile el ${formatoFecha.format(fecha)} a las ${formatoHora.format(fecha)}.",
            MARGEN, 766f, pie
        )
        canvas.drawLine(DERECHA - 170f, 752f, DERECHA, 752f, linea(TINTA, 1f))
        val firma = texto(9f, SUAVE).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("Firma del Anfitrión", DERECHA - 85f, 766f, firma)

        // 8) Pie de página
        canvas.drawLine(0f, 808f, ANCHO.toFloat(), 808f, linea(LINEA, 1f))
        canvas.drawText("Documento generado por TECNISIS Mobile", MARGEN, 829f, texto(9f, SUAVE))
        canvas.drawText("Página 1 de 1", DERECHA, 829f, derecha)
    }

    // ---------- HU09: documento de rechazo de una obra ----------

    /** HU09: escribe en [destino] el documento de rechazo de una obra. Las dos copias solo cambian la franja. */
    fun generarRechazoObra(documento: DocumentoRechazoObra, copia: CopiaDocumento, destino: File) {
        val pdf = PdfDocument()
        try {
            val pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
            dibujarRechazoObra(pagina.canvas, documento, copia)
            pdf.finishPage(pagina)
            destino.parentFile?.mkdirs()
            FileOutputStream(destino).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    private fun dibujarRechazoObra(canvas: Canvas, d: DocumentoRechazoObra, copia: CopiaDocumento) {
        val fecha = Date(d.fecha)

        // 1) Franja roja con la copia
        canvas.drawRect(0f, 0f, ANCHO.toFloat(), 30f, relleno(ROJO))
        val franja = texto(8.5f, BLANCO, negrita = true).apply { letterSpacing = 0.14f; textAlign = Paint.Align.CENTER }
        canvas.drawText(copia.etiqueta.uppercase(locale), ANCHO / 2f, 19f, franja)

        // 2) Encabezado: logo, galería, número de la obra y fecha
        dibujarLogo(canvas, MARGEN, 46f, 34f)
        canvas.drawText("TECNISIS Galería", MARGEN + 44f, 62f, texto(14f, TINTA, negrita = true, serif = true))
        canvas.drawText("Exposiciones de arte y cultura peruana", MARGEN + 44f, 76f, texto(9f, SUAVE))
        val derecha = texto(9f, SUAVE).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Obra N.° ${d.numero}", DERECHA, 60f, derecha)
        canvas.drawText("Fecha: ${formatoFecha.format(fecha)} · ${formatoHora.format(fecha)}", DERECHA, 74f, derecha)

        // 3) Título e introducción
        canvas.drawLine(MARGEN, 100f, DERECHA, 100f, linea(TINTA, 2f))
        var y = 112f
        y += parrafo(canvas, d.tituloDocumento, MARGEN, y, ANCHO_UTIL, texto(17f, TINTA, negrita = true, serif = true)) + 6f
        y += parrafo(canvas, d.introduccion, MARGEN, y, ANCHO_UTIL, texto(10f, TEXTO)) + 16f

        // 4) Socio (solo nombre y número) y solicitud
        canvas.drawRoundRect(RectF(MARGEN, y, DERECHA, y + 76f), 8f, 8f, relleno(PAPEL))
        dato(canvas, "Socio", d.nombreSocio, MARGEN + 16f, y + 20f)
        dato(canvas, "N.° de socio", d.numeroSocio, MARGEN + 270f, y + 20f)
        dato(canvas, "Solicitud N.°", d.numeroSolicitud, MARGEN + 16f, y + 52f)
        dato(canvas, "Asignada el", d.fechaAsignacion?.let { formatoFecha.format(Date(it)) }.orEmpty(), MARGEN + 270f, y + 52f)
        y += 96f

        // 5) Tabla con la obra
        val columnas = floatArrayOf(MARGEN, MARGEN + 22f, MARGEN + 222f, MARGEN + 312f, MARGEN + 402f)
        val cabecera = texto(9f, SUAVE, negrita = true)
        listOf("#", "Obra", "Técnica", "Medidas (cm)", "Estado")
            .forEachIndexed { i, titulo -> canvas.drawText(titulo, columnas[i], y, cabecera) }
        y += 7f
        canvas.drawLine(MARGEN, y, DERECHA, y, linea(TINTA, 1.5f))
        y += 18f
        listOf("1", d.titulo, d.tecnica, d.medidas, EstadoObra.RECHAZADA.etiqueta).forEachIndexed { i, valor ->
            val ancho = (if (i < columnas.size - 1) columnas[i + 1] else DERECHA) - columnas[i] - 8f
            val pintura = texto(10f, TINTA, negrita = i == 1)
            canvas.drawText(recortar(valor, pintura, ancho), columnas[i], y, pintura)
        }
        y += 9f
        canvas.drawLine(MARGEN, y, DERECHA, y, linea(LINEA, 1f))

        // 6) Motivo del rechazo, en un recuadro rojo
        y += 20f
        val cuerpo = StaticLayout.Builder
            .obtain(d.motivo, 0, d.motivo.length, texto(10.5f, TINTA), (ANCHO_UTIL - 32f).toInt())
            .setLineSpacing(0f, 1.25f)
            .build()
        val alto = 34f + cuerpo.height
        canvas.drawRoundRect(RectF(MARGEN, y, DERECHA, y + alto), 8f, 8f, linea(ROJO, 1.5f))
        val etiqueta = texto(9f, ROJO, negrita = true).apply { letterSpacing = 0.08f }
        canvas.drawText("MOTIVO DEL RECHAZO", MARGEN + 16f, y + 20f, etiqueta)
        canvas.save()
        canvas.translate(MARGEN + 16f, y + 26f)
        cuerpo.draw(canvas)
        canvas.restore()

        // 7) Quién evaluó y firma
        val pie = texto(9.5f, TEXTO)
        canvas.drawText("Evaluado por: Especialista de Arte · ${d.evaluadoPor}", MARGEN, 752f, pie)
        canvas.drawText("Registrado en TECNISIS Mobile el ${formatoFecha.format(fecha)} · ${formatoHora.format(fecha)}.", MARGEN, 766f, pie)
        canvas.drawLine(DERECHA - 170f, 752f, DERECHA, 752f, linea(TINTA, 1f))
        val firma = texto(9f, SUAVE).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("Firma del Especialista de Arte", DERECHA - 85f, 766f, firma)

        // 8) Pie de página
        canvas.drawLine(0f, 808f, ANCHO.toFloat(), 808f, linea(LINEA, 1f))
        canvas.drawText("Documento generado por TECNISIS Mobile", MARGEN, 829f, texto(9f, SUAVE))
        canvas.drawText("Página 1 de 1", DERECHA, 829f, derecha)
    }

    /** El logo "Marco con pigmentos": marco claro con borde oscuro y tres círculos (cobalto, cadmio, bermellón). */
    private fun dibujarLogo(canvas: Canvas, x: Float, y: Float, lado: Float) {
        val e = lado / 36f
        val marco = RectF(x + 4 * e, y + 4 * e, x + 32 * e, y + 32 * e)
        canvas.drawRoundRect(marco, 3 * e, 3 * e, relleno(LIENZO))
        canvas.drawRoundRect(marco, 3 * e, 3 * e, linea(MARCO, 3 * e))
        canvas.drawCircle(x + 14 * e, y + 15 * e, 5 * e, relleno(COBALTO))
        canvas.drawCircle(x + 22 * e, y + 17 * e, 4.4f * e, relleno(CADMIO))
        canvas.drawCircle(x + 18 * e, y + 24 * e, 4.4f * e, relleno(BERMELLON))
    }

    /** Rótulo gris y valor en negrita, uno debajo del otro. */
    private fun dato(canvas: Canvas, rotulo: String, valor: String, x: Float, y: Float) {
        canvas.drawText(rotulo, x, y, texto(9f, SUAVE))
        val pintura = texto(10.5f, TINTA, negrita = true)
        canvas.drawText(recortar(valor.ifBlank { "—" }, pintura, 235f), x, y + 14f, pintura)
    }

    /** Texto con saltos de línea automáticos. Devuelve el alto que ocupó. */
    private fun parrafo(canvas: Canvas, texto: String, x: Float, y: Float, ancho: Float, pintura: TextPaint): Float {
        val capa = StaticLayout.Builder.obtain(texto, 0, texto.length, pintura, ancho.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        capa.draw(canvas)
        canvas.restore()
        return capa.height.toFloat()
    }

    /** Si no entra en la columna, lo corta con "…". */
    private fun recortar(valor: String, pintura: TextPaint, ancho: Float): String =
        TextUtils.ellipsize(valor, pintura, ancho, TextUtils.TruncateAt.END).toString()

    private fun texto(tamano: Float, color: Int, negrita: Boolean = false, serif: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.color = color
            typeface = Typeface.create(
                if (serif) Typeface.SERIF else Typeface.SANS_SERIF,
                if (negrita) Typeface.BOLD else Typeface.NORMAL
            )
        }

    private fun relleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun linea(color: Int, grosor: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = grosor
    }

    private companion object {
        const val ANCHO = 595 // A4 en puntos (1/72 de pulgada)
        const val ALTO = 842
        const val MARGEN = 44f
        const val DERECHA = ANCHO - MARGEN
        const val ANCHO_UTIL = DERECHA - MARGEN

        // Paleta de pigmentos (ARGB)
        const val COBALTO = 0xFF3A4DB5.toInt()
        const val CADMIO = 0xFFEBB54A.toInt()
        const val BERMELLON = 0xFFD9553F.toInt()
        const val ROJO = 0xFF9A3322.toInt()
        const val MARCO = 0xFF121320.toInt()
        const val LIENZO = 0xFFFBF6EC.toInt()
        const val TINTA = 0xFF17172B.toInt()
        const val TEXTO = 0xFF3D3A45.toInt()
        const val SUAVE = 0xFF5B5752.toInt()
        const val PAPEL = 0xFFF6F2EA.toInt()
        const val LINEA = 0xFFE4DDD0.toInt()
        const val BLANCO = 0xFFFFFFFF.toInt()
    }
}