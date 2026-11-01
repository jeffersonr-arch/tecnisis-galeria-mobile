package com.tecnisis.galeria.domain.model

/** HU06: técnicas que acepta la galería. valorFirestore es lo que se guarda (sin tildes ni espacios). */
enum class Tecnica(val valorFirestore: String, val etiqueta: String) {
    OLEO("oleo", "Óleo"),
    ACRILICO("acrilico", "Acrílico"),
    ACUARELA("acuarela", "Acuarela"),
    TECNICA_MIXTA("tecnica_mixta", "Técnica mixta"),
    DIBUJO("dibujo", "Dibujo"),
    GRABADO("grabado", "Grabado"),
    ESCULTURA("escultura", "Escultura"),
    FOTOGRAFIA("fotografia", "Fotografía"),
    CERAMICA("ceramica", "Cerámica"),
    TEXTIL("textil", "Textil");

    companion object {
        fun desdeValor(valor: String?): Tecnica? = entries.firstOrNull { it.valorFirestore == valor }
    }
}