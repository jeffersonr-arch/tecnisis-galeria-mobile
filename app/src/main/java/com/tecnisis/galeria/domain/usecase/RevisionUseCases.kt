package com.tecnisis.galeria.domain.usecase

import com.tecnisis.galeria.domain.repository.RevisionRepository

/** HU07: casos de uso del Anfitrión para revisar solicitudes de exhibición. */
class ObservarSolicitudesUseCase(private val repo: RevisionRepository) {
    operator fun invoke(resueltas: Boolean) = repo.observarSolicitudes(resueltas)
}

class ObservarSolicitudEnRevisionUseCase(private val repo: RevisionRepository) {
    operator fun invoke(id: String) = repo.observarSolicitud(id)
}

class ObservarExpertosUseCase(private val repo: RevisionRepository) {
    operator fun invoke() = repo.observarExpertos()
}

class AnalizarSolicitudUseCase(private val repo: RevisionRepository) {
    suspend operator fun invoke(id: String) = repo.analizar(id)
}

class DescargarFotoObraUseCase(private val repo: RevisionRepository) {
    suspend operator fun invoke(ruta: String) = repo.descargarFoto(ruta)
}