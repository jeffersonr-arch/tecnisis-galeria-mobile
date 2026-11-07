package com.tecnisis.galeria.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.tecnisis.galeria.data.local.GaleriaDatabase
import com.tecnisis.galeria.data.local.ObraEntity
import com.tecnisis.galeria.data.local.SolicitudEntity
import com.tecnisis.galeria.data.local.aDominio
import com.tecnisis.galeria.domain.model.DatosObra
import com.tecnisis.galeria.domain.model.EstadoObra
import com.tecnisis.galeria.domain.model.EstadoSolicitud
import com.tecnisis.galeria.domain.model.MAX_OBRAS_POR_SOLICITUD
import com.tecnisis.galeria.domain.model.Obra
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.model.SolicitudExhibicion
import com.tecnisis.galeria.domain.repository.FotoRepository
import com.tecnisis.galeria.domain.repository.ObraRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

/** HU06: el borrador vive solo en Room (no necesita internet). La subida la hace EnvioRepositoryImpl. */
class ObraRepositoryImpl(
    private val auth: FirebaseAuth,
    private val baseDeDatos: GaleriaDatabase,
    private val fotos: FotoRepository
) : ObraRepository {

    private val dao = baseDeDatos.obraDao()

    override fun observarMiSolicitud(): Flow<SolicitudExhibicion?> {
        val uid = auth.currentUser?.uid ?: return flowOf(null)
        return dao.observarUltima(uid).map { it?.aDominio() }
    }

    override suspend fun obtenerObra(id: String): Result<Obra> = ejecutar {
        dao.obra(id)?.aDominio() ?: throw ObraError.NoEncontrada
    }

    override suspend fun guardarObra(id: String?, datos: DatosObra): Result<String> = ejecutar {
        val uid = auth.currentUser?.uid ?: throw ObraError.SinSesion
        var fotoReemplazada: String? = null

        // Transacción: revisar el estado y guardar pasan juntos o no pasa nada
        val idGuardado = baseDeDatos.withTransaction {
            if (id == null) {
                val solicitud = solicitudParaAgregar(uid)
                val nueva = ObraEntity(
                    id = UUID.randomUUID().toString(),
                    solicitudId = solicitud.id,
                    uid = uid,
                    titulo = datos.titulo,
                    tecnica = datos.tecnica.valorFirestore,
                    altoCm = datos.altoCm,
                    anchoCm = datos.anchoCm,
                    profundidadCm = datos.profundidadCm,
                    fotoLocal = datos.fotoLocal,
                    fotoUrl = null,
                    nota = datos.nota,
                    estado = EstadoObra.BORRADOR.valorFirestore,
                    creadaEn = System.currentTimeMillis()
                )
                dao.guardarObra(nueva)
                nueva.id
            } else {
                val actual = dao.obra(id) ?: throw ObraError.NoEncontrada
                exigirBorrador(dao.solicitud(actual.solicitudId))
                if (actual.fotoLocal != datos.fotoLocal) fotoReemplazada = actual.fotoLocal
                dao.guardarObra(
                    actual.copy(
                        titulo = datos.titulo,
                        tecnica = datos.tecnica.valorFirestore,
                        altoCm = datos.altoCm,
                        anchoCm = datos.anchoCm,
                        profundidadCm = datos.profundidadCm,
                        fotoLocal = datos.fotoLocal,
                        nota = datos.nota
                    )
                )
                id
            }
        }
        fotoReemplazada?.let { fotos.borrarFoto(it) } // la foto vieja ya no la usa nadie
        idGuardado
    }

    override suspend fun eliminarObra(id: String): Result<Unit> = ejecutar {
        val obra = dao.obra(id) ?: throw ObraError.NoEncontrada
        baseDeDatos.withTransaction {
            exigirBorrador(dao.solicitud(obra.solicitudId))
            dao.borrarObra(id)
            // Un borrador sin obras no sirve: se borra para que Mis obras vuelva a "sin obras"
            if (dao.contarObras(obra.solicitudId) == 0) dao.borrarSolicitud(obra.solicitudId)
        }
        fotos.borrarFoto(obra.fotoLocal)
    }

    override suspend fun marcarParaEnvio(): Result<Unit> = ejecutar {
        val uid = auth.currentUser?.uid ?: throw ObraError.SinSesion
        baseDeDatos.withTransaction {
            val ultima = dao.ultimaSolicitud(uid) ?: throw ObraError.SinObras
            exigirBorrador(ultima)
            if (dao.contarObras(ultima.id) == 0) throw ObraError.SinObras
            dao.cambiarEstadoSolicitud(ultima.id, EstadoSolicitud.POR_ENVIAR.valorFirestore, null)
        }
    }

    /** El borrador abierto, o uno nuevo si no hay (o si la última solicitud ya se cerró en la HU07). */
    private suspend fun solicitudParaAgregar(uid: String): SolicitudEntity {
        val ultima = dao.ultimaSolicitud(uid)
        if (ultima != null) {
            val estado = EstadoSolicitud.desdeValor(ultima.estado)
            if (estado == EstadoSolicitud.BORRADOR) {
                if (dao.contarObras(ultima.id) >= MAX_OBRAS_POR_SOLICITUD) throw ObraError.LimiteDeObras
                return ultima
            }
            if (estado.estaAbierta) throw ObraError.SolicitudNoEditable // pendiente de envío o enviada
        }
        val nueva = SolicitudEntity(
            id = UUID.randomUUID().toString(),
            uid = uid,
            estado = EstadoSolicitud.BORRADOR.valorFirestore,
            creadaEn = System.currentTimeMillis(),
            enviadaEn = null
        )
        dao.guardarSolicitud(nueva)
        return nueva
    }

    private fun exigirBorrador(solicitud: SolicitudEntity?) {
        if (solicitud == null) throw ObraError.NoEncontrada
        if (EstadoSolicitud.desdeValor(solicitud.estado) != EstadoSolicitud.BORRADOR) {
            throw ObraError.SolicitudNoEditable
        }
    }

    /** Convierte cualquier error en un ObraError; lo inesperado queda en Logcat. */
    private inline fun <T> ejecutar(bloque: () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ObraError) {
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(TAG, "Error inesperado en Room", e)
        Result.failure(ObraError.Desconocido)
    }

    private companion object {
        const val TAG = "ObraRepository"
    }
}