package com.tecnisis.galeria.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.tecnisis.galeria.domain.model.AuthError
import com.tecnisis.galeria.domain.model.Rol
import com.tecnisis.galeria.domain.model.Usuario
import com.tecnisis.galeria.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario> =
        ejecutarSeguro {
            val uid = auth.signInWithEmailAndPassword(correo, contrasena).await().user?.uid
                ?: throw AuthError.Desconocido
            leerUsuario(uid)
        }

    override suspend fun obtenerUsuarioActual(): Result<Usuario> =
        ejecutarSeguro {
            val uid = auth.currentUser?.uid ?: throw AuthError.SinSesion
            leerUsuario(uid)
        }

    override fun haySesionActiva(): Boolean = auth.currentUser != null

    override fun cerrarSesion() = auth.signOut()

    private suspend fun leerUsuario(uid: String): Usuario {
        val doc = db.collection(COLECCION_USUARIOS).document(uid).get().await()
        val rol = Rol.desdeFirestore(doc.getString("rol"))
        if (!doc.exists() || rol == null) {
            auth.signOut() // sin rol válido no debe quedar sesión abierta
            throw AuthError.SinRol
        }
        return Usuario(
            uid = uid,
            nombre = doc.getString("nombre").orEmpty(),
            correo = doc.getString("correo").orEmpty(),
            rol = rol
        )
    }

    private inline fun <T> ejecutarSeguro(bloque: () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(mapearError(e))
        }

    private fun mapearError(e: Exception): AuthError = when (e) {
        is AuthError -> e
        is FirebaseAuthInvalidUserException ->
            if (e.errorCode == "ERROR_USER_DISABLED") AuthError.UsuarioDeshabilitado
            else AuthError.CredencialesInvalidas
        is FirebaseAuthInvalidCredentialsException -> AuthError.CredencialesInvalidas
        is FirebaseNetworkException -> AuthError.SinConexion
        is FirebaseTooManyRequestsException -> AuthError.DemasiadosIntentos
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> AuthError.SinConexion
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AuthError.SinRol
            else -> AuthError.Desconocido
        }
        else -> AuthError.Desconocido
    }

    private companion object {
        const val COLECCION_USUARIOS = "usuarios"
    }
}