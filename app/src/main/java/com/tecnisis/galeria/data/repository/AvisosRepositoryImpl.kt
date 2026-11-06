package com.tecnisis.galeria.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.tecnisis.galeria.domain.repository.AvisosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** HU07: guarda el token FCM del celular en tokens/{uid}; la Cloud Function lo usa para avisar al socio. */
class AvisosRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val alcance: CoroutineScope
) : AvisosRepository {

    override fun registrarToken() {
        alcance.launch {
            intentar { guardar(FirebaseMessaging.getInstance().token.await()) }
        }
    }

    override fun guardarToken(token: String) {
        alcance.launch { intentar { guardar(token) } }
    }

    override fun olvidarToken() {
        // Invalida el token en los servidores de FCM: los avisos para esta cuenta dejan de llegar a este celular.
        // La próxima vez que entre un socio, FCM entrega un token nuevo.
        FirebaseMessaging.getInstance().deleteToken()
            .addOnFailureListener { Log.w(TAG, "No se pudo borrar el token", it) }
    }

    private suspend fun guardar(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(TOKENS).document(uid)
            .set(mapOf("token" to token, "actualizado" to FieldValue.serverTimestamp()))
            .await()
    }

    /** Sin internet o sin Google Play no se registra; no es un error para el usuario. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar el token", e)
        }
    }

    private companion object {
        const val TAG = "AvisosFcm"
        const val TOKENS = "tokens"
    }
}