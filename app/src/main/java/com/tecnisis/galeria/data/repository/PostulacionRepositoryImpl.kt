package com.tecnisis.galeria.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.tecnisis.galeria.domain.model.DecisionPostulacion
import com.tecnisis.galeria.domain.model.EstadoPostulacion
import com.tecnisis.galeria.domain.model.Postulacion
import com.tecnisis.galeria.domain.model.PostulacionError
import com.tecnisis.galeria.domain.repository.PostulacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

class PostulacionRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : PostulacionRepository {

    private val postulaciones get() = db.collection("postulaciones")

    override suspend fun enviar(postulacion: Postulacion): Result<String> = ejecutar {
        val uid = auth.currentUser?.uid ?: throw PostulacionError.SinPermiso
        val refDni = db.collection("dnis").document(postulacion.dni)
        if (refDni.get().await().exists()) throw PostulacionError.DniYaRegistrado
        val refPostulacion = postulaciones.document(uid)
        if (refPostulacion.get().await().exists()) throw PostulacionError.YaPostulo

        // Las dos escrituras van juntas: o se guardan ambas o ninguna
        db.batch()
            .set(refDni, mapOf("uid" to uid))
            .set(refPostulacion, aMapa(postulacion.copy(uid = uid)))
            .commit()
            .await()
        uid
    }

    override fun observarMiPostulacion(): Flow<Postulacion?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registro = postulaciones.document(uid).addSnapshotListener { doc, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(if (doc != null && doc.exists()) doc.aPostulacion() else null)
        }
        awaitClose { registro.remove() }
    }

    override suspend fun obtener(uid: String): Result<Postulacion> = ejecutar {
        val doc = postulaciones.document(uid).get().await()
        if (!doc.exists()) throw PostulacionError.NoEncontrada
        doc.aPostulacion()
    }

    override suspend fun registrarVerificacion(
        uid: String, estado: EstadoPostulacion, motivo: String?
    ): Result<Unit> = ejecutar {
        postulaciones.document(uid).update(
            mapOf(
                "estado" to estado.valorFirestore,
                "motivo" to motivo,
                "fechaVerificacion" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    override fun observarPorEstado(estado: EstadoPostulacion): Flow<List<Postulacion>> = callbackFlow {
        val registro = postulaciones
            .whereEqualTo("estado", estado.valorFirestore)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapear(error))
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents.orEmpty()
                    .map { it.aPostulacion() }
                    .sortedBy { it.fechaRegistro ?: 0L }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    // HU04: la Cloud Function hace todo en una transacción (postulación, socio, contador y rol)
    override suspend fun resolver(
        uid: String, decision: DecisionPostulacion, mensaje: String?
    ): Result<Unit> = ejecutar {
        functions.getHttpsCallable("resolverPostulacion")
            .call(
                mapOf(
                    "uid" to uid,
                    "decision" to decision.valorFuncion,
                    "mensaje" to mensaje
                )
            )
            .await()
        Unit
    }

    private fun aMapa(p: Postulacion) = mapOf(
        "uid" to p.uid,
        "dni" to p.dni,
        "nombres" to p.nombres.trim(),
        "apellidoPaterno" to p.apellidoPaterno.trim(),
        "apellidoMaterno" to p.apellidoMaterno.trim(),
        "direccion" to p.direccion.trim(),
        "sexo" to p.sexo,
        "telefono" to p.telefono,
        "correo" to p.correo.trim(),
        "estado" to EstadoPostulacion.PENDIENTE_VERIFICACION.valorFirestore,
        "fechaRegistro" to FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.aPostulacion() = Postulacion(
        uid = id,
        dni = getString("dni").orEmpty(),
        nombres = getString("nombres").orEmpty(),
        apellidoPaterno = getString("apellidoPaterno").orEmpty(),
        apellidoMaterno = getString("apellidoMaterno").orEmpty(),
        direccion = getString("direccion").orEmpty(),
        sexo = getString("sexo").orEmpty(),
        telefono = getString("telefono").orEmpty(),
        correo = getString("correo").orEmpty(),
        estado = EstadoPostulacion.desdeFirestore(getString("estado")),
        motivo = getString("motivo"),
        motivoRechazo = getString("motivoRechazo"),
        fechaRegistro = getTimestamp("fechaRegistro")?.toDate()?.time,
        mensajeAnfitrion = getString("mensajeAnfitrion"),
        notaAprobacion = getString("notaAprobacion"),
        numeroSocio = getString("numeroSocio")
    )

    private inline fun <T> ejecutar(bloque: () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(mapear(e))
        }

    private fun mapear(e: Exception): PostulacionError = when (e) {
        is PostulacionError -> e
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> PostulacionError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> PostulacionError.SinPermiso
            else -> PostulacionError.Desconocido
        }
        is FirebaseFunctionsException -> when {
            e.cause is IOException -> PostulacionError.SinConexion // sin internet
            e.code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                    e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> PostulacionError.SinConexion
            e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> PostulacionError.SinPermiso
            e.code == FirebaseFunctionsException.Code.NOT_FOUND -> PostulacionError.NoEncontrada
            e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION -> PostulacionError.DecisionNoPermitida
            else -> PostulacionError.Servidor(e.message ?: "Error en el servidor")
        }
        is FirebaseNetworkException -> PostulacionError.SinConexion
        else -> PostulacionError.Desconocido
    }
}