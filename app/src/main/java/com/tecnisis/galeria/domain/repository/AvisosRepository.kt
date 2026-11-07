package com.tecnisis.galeria.domain.repository

/** HU07: el token de Firebase Cloud Messaging, para que la Cloud Function sepa a qué celular avisar. */
interface AvisosRepository {
    /** Pide el token actual y lo guarda en tokens/{uid}. Si falla, no pasa nada: se intenta en el próximo inicio. */
    fun registrarToken()

    /** FCM entregó un token nuevo (se reinstaló la app o se borraron sus datos). */
    fun guardarToken(token: String)

    /** Al cerrar sesión: invalida el token del celular para que no lleguen avisos de esta cuenta. */
    fun olvidarToken()
}