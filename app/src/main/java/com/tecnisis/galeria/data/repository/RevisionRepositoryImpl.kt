package com.tecnisis.galeria.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.tecnisis.galeria.data.remote.AnalisisApi
import com.tecnisis.galeria.data.remote.PedidoAnalisisDto
import com.tecnisis.galeria.domain.model.CandidatoExperto
import com.tecnisis.galeria.domain.model.EstadoAnalisis
import com.tecnisis.galeria.domain.model.EstadoEspecialista
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.PerfilEspecialista
import com.tecnisis.galeria.domain.model.ObraEnRevision
import com.tecnisis.galeria.domain.model.ObraResumen
import com.tecnisis.galeria.domain.model.RevisionError
import com.tecnisis.galeria.domain.model.SolicitudEnRevision
import com.tecnisis.galeria.domain.model.SolicitudResumen
import com.tecnisis.galeria.domain.model.Tecnica
import com.tecnisis.galeria.domain.repository.RevisionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** HU07: Firestore en tiempo real para el Anfitrión, Retrofit para el análisis y Storage para las fotos. */
@OptIn(ExperimentalCoroutinesApi::class)
class RevisionRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val api: AnalisisApi
) : RevisionRepository {

    /** Nombre y número de cada socio ya leído (uid → par), para no pedirlo en cada cambio. */
    private val socios = mutableMapOf<String, Pair<String, String>>()

    override fun observarSolicitudes(resueltas: Boolean): Flow<List<SolicitudResumen>> {
        val estados = if (resueltas) {
            listOf(EstadoSolicitud.APROBADA.valorFirestore, EstadoSolicitud.RECHAZADA.valorFirestore)
        } else {
            listOf(EstadoSolicitud.ENVIADA.valorFirestore)
        }
        return documentos(db.collection(SOLICITUDES).whereIn("estado", estados))
            .mapLatest { docs -> leer { armarResumenes(docs, resueltas) } }
    }

    override fun observarSolicitud(id: String): Flow<SolicitudEnRevision?> =
        combine(
            documento(db.collection(SOLICITUDES).document(id)),
            documentos(db.collection(OBRAS).whereEqualTo("solicitudId", id))
        ) { solicitud, obras -> solicitud to obras }
            .mapLatest { (solicitud, obras) ->
                if (solicitud == null) null else leer { armarSolicitud(solicitud, obras) }
            }

    override fun observarExpertos(): Flow<List<PerfilEspecialista>> = callbackFlow {
        val registro = db.collection(EXPERTOS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { it.aExperto() }.sortedBy { it.nombre })
        }
        awaitClose { registro.remove() }
    }

    override suspend fun analizar(id: String): Result<EstadoAnalisis> = ejecutar {
        val usuario = auth.currentUser ?: throw RevisionError.SinPermiso
        val token = usuario.getIdToken(false).await().token ?: throw RevisionError.SinPermiso
        val respuesta = api.analizar("Bearer $token", PedidoAnalisisDto(id))
        when (respuesta.code()) {
            200, 202 -> EstadoAnalisis.desdeValor(respuesta.body()?.analisis)
            401, 403 -> throw RevisionError.SinPermiso
            404 -> throw RevisionError.NoEncontrada
            409 -> throw RevisionError.YaResuelta
            else -> throw RevisionError.Servidor("código ${respuesta.code()}")
        }
    }

    override suspend fun descargarFoto(ruta: String): Result<ByteArray> = ejecutar {
        storage.reference.child(ruta).getBytes(MAXIMO_FOTO).await()
    }

    // ---------- Lectura de documentos ----------

    private suspend fun armarResumenes(docs: List<DocumentSnapshot>, resueltas: Boolean): List<SolicitudResumen> {
        // Las obras de todas las solicitudes, de 30 en 30 (límite de whereIn)
        val obrasPorSolicitud = docs.map { it.id }.chunked(30).flatMap { ids ->
            db.collection(OBRAS).whereIn("solicitudId", ids).get().await().documents
        }.groupBy { it.getString("solicitudId").orEmpty() }

        val lista = docs.map { doc ->
            val (nombre, numero) = socio(doc.getString("uid").orEmpty())
            SolicitudResumen(
                id = doc.id,
                nombreSocio = nombre,
                numeroSocio = numero,
                estado = EstadoSolicitud.desdeValor(doc.getString("estado")),
                analisis = EstadoAnalisis.desdeValor(doc.getString("analisis")),
                obrasSinExperto = doc.getLong("obrasSinExperto")?.toInt(),
                fechaEnvio = doc.getTimestamp("fechaEnvio")?.toDate()?.time,
                obras = obrasPorSolicitud[doc.id].orEmpty()
                    .sortedBy { it.getTimestamp("fechaRegistro")?.toDate()?.time ?: 0L }
                    .map { o ->
                        ObraResumen(
                            id = o.id,
                            tecnica = Tecnica.desdeValor(o.getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
                            fotoRuta = o.getString("fotoRuta").orEmpty()
                        )
                    }
            )
        }
        // Por revisar: la más antigua primero (orden de llegada). Resueltas: la más reciente primero.
        return if (resueltas) lista.sortedByDescending { it.fechaEnvio ?: 0L } else lista.sortedBy { it.fechaEnvio ?: 0L }
    }

    private suspend fun armarSolicitud(doc: DocumentSnapshot, obras: List<DocumentSnapshot>): SolicitudEnRevision {
        val uid = doc.getString("uid").orEmpty()
        val (nombre, numero) = socio(uid)
        return SolicitudEnRevision(
            id = doc.id,
            uidSocio = uid,
            nombreSocio = nombre,
            numeroSocio = numero,
            estado = EstadoSolicitud.desdeValor(doc.getString("estado")),
            analisis = EstadoAnalisis.desdeValor(doc.getString("analisis")),
            analisisDesde = doc.getTimestamp("analisisDesde")?.toDate()?.time,
            fechaEnvio = doc.getTimestamp("fechaEnvio")?.toDate()?.time,
            obras = obras
                .sortedBy { it.getTimestamp("fechaRegistro")?.toDate()?.time ?: 0L }
                .map { it.aObraEnRevision() },
            motivoRechazo = doc.getString("motivoRechazo"),                         // Parte 2
            fechaResolucion = doc.getTimestamp("fechaResolucion")?.toDate()?.time   // Parte 2
        )
    }

    /** Nombre y número de socio; si no se puede leer, quedan vacíos (la pantalla igual funciona). */
    private suspend fun socio(uid: String): Pair<String, String> {
        socios[uid]?.let { return it }
        if (uid.isEmpty()) return "Socio" to ""
        val doc = db.collection("socios").document(uid).get().await()
        val par = (doc.getString("nombreCompleto") ?: "Socio") to doc.getString("numeroSocio").orEmpty()
        socios[uid] = par
        return par
    }

    private fun DocumentSnapshot.aObraEnRevision() = ObraEnRevision(
        id = id,
        titulo = getString("titulo").orEmpty(),
        tecnica = Tecnica.desdeValor(getString("tecnica")) ?: Tecnica.TECNICA_MIXTA,
        altoCm = getDouble("altoCm") ?: 0.0,
        anchoCm = getDouble("anchoCm") ?: 0.0,
        profundidadCm = getDouble("profundidadCm"),
        nota = getString("nota").orEmpty(),
        fotoRuta = getString("fotoRuta").orEmpty(),
        descripcionIa = getString("descripcionIa"),
        candidatos = (get("candidatos") as? List<*>).orEmpty().mapNotNull { elemento ->
            val mapa = elemento as? Map<*, *> ?: return@mapNotNull null
            val uid = mapa["uid"] as? String ?: return@mapNotNull null
            CandidatoExperto(uid, (mapa["afinidad"] as? Number)?.toInt() ?: 0)
        },
        recomendadoUid = getString("recomendadoUid"),
        expertoAsignadoUid = getString("expertoUid"), // Parte 2: lo escribe decidirSolicitud al aprobar
        estado = EstadoObra.desdeValor(getString("estado")), // HU08: el Anfitrión ve el veredicto de cada obra
        documentoRechazo = (get("documentoRechazo") as? List<*>).orEmpty().filterIsInstance<String>() // HU09
    )

    private fun DocumentSnapshot.aExperto() = PerfilEspecialista(
        uid = id,
        nombre = getString("nombre").orEmpty(),
        tecnicas = (get("tecnicas") as? List<*>).orEmpty()
            .mapNotNull { Tecnica.desdeValor(it as? String) }
            .toSet(),
        estado = EstadoEspecialista.desdeValor(getString("estado")),
        carga = getLong("carga")?.toInt() ?: 0
    )

    // ---------- Tiempo real ----------

    private fun documentos(consulta: Query): Flow<List<DocumentSnapshot>> = callbackFlow {
        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty())
        }
        awaitClose { registro.remove() }
    }

    private fun documento(ref: DocumentReference): Flow<DocumentSnapshot?> = callbackFlow {
        val registro = ref.addSnapshotListener { doc, error ->
            if (error != null) {
                close(mapear(error))
                return@addSnapshotListener
            }
            trySend(if (doc != null && doc.exists()) doc else null)
        }
        awaitClose { registro.remove() }
    }

    // ---------- Errores ----------

    /** Dentro de un Flow: convierte cualquier excepción en RevisionError (el ViewModel la muestra). */
    private suspend fun <T> leer(bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw mapear(e)
        }

    private suspend fun <T> ejecutar(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(mapear(e))
        }

    private fun mapear(e: Exception): RevisionError = when (e) {
        is RevisionError -> e
        is IOException -> RevisionError.SinConexion // sin internet o pasó el tiempo de espera
        is FirebaseNetworkException -> RevisionError.SinConexion
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> RevisionError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> RevisionError.SinPermiso
            else -> RevisionError.Desconocido
        }
        is StorageException -> when (e.errorCode) {
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> RevisionError.SinConexion
            StorageException.ERROR_NOT_AUTHORIZED,
            StorageException.ERROR_NOT_AUTHENTICATED -> RevisionError.SinPermiso
            StorageException.ERROR_OBJECT_NOT_FOUND -> RevisionError.NoEncontrada
            else -> RevisionError.Desconocido
        }
        else -> RevisionError.Desconocido
    }

    companion object {
        private const val SOLICITUDES = "solicitudes"
        private const val OBRAS = "obras"
        private const val EXPERTOS = "expertos"
        private const val MAXIMO_FOTO = 5L * 1024 * 1024 // las reglas de Storage ya limitan a 5 MB
    }
}