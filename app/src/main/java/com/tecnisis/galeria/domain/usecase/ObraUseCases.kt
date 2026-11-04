package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.model.FormularioObra
import com.tecnisis.galeria.domain.model.ObraError
import com.tecnisis.galeria.domain.repository.EnvioRepository
import com.tecnisis.galeria.domain.repository.EnvioSolicitudScheduler
import com.tecnisis.galeria.domain.repository.FotoRepository
import com.tecnisis.galeria.domain.repository.ObraRepository

class ObservarMiSolicitudUseCase(private val repo: ObraRepository) {
    operator fun invoke() = repo.observarMiSolicitud()
}

class ObtenerObraUseCase(private val repo: ObraRepository) {
    suspend operator fun invoke(id: String) = repo.obtenerObra(id)
}

/** Verifica primero: si algo está mal no toca Room y devuelve todos los campos con error. */
class GuardarObraUseCase(private val repo: ObraRepository) {
    suspend operator fun invoke(formulario: FormularioObra): Result<String> {
        val campos = formulario.camposInvalidos()
        if (campos.isNotEmpty()) return Result.failure(ObraError.DatosInvalidos(campos))
        return repo.guardarObra(formulario.id, formulario.aDatos())
    }
}

class EliminarObraUseCase(private val repo: ObraRepository) {
    suspend operator fun invoke(id: String) = repo.eliminarObra(id)
}

/** Marca la solicitud y programa WorkManager. Si no se pudo marcar, no programa nada. */
class EnviarSolicitudUseCase(
    private val obras: ObraRepository,
    private val envio: EnvioSolicitudScheduler
) {
    suspend operator fun invoke(): Result<Unit> = obras.marcarParaEnvio().onSuccess { envio.programar() }
}

class SubirSolicitudesUseCase(private val envio: EnvioRepository) {
    suspend operator fun invoke() = envio.subirPendientes()
}

class CrearDestinoCamaraUseCase(private val fotos: FotoRepository) {
    operator fun invoke() = fotos.crearDestinoCamara()
}

class PrepararFotoUseCase(private val fotos: FotoRepository) {
    suspend operator fun invoke(origen: String) = fotos.prepararFoto(origen)
}

class DescartarFotoUseCase(private val fotos: FotoRepository) {
    operator fun invoke(ruta: String) = fotos.borrarFoto(ruta)
}