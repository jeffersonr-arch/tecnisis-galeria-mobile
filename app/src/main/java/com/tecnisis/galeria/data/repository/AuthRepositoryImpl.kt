package com.tecnisis.galeria.data.repository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

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
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    override suspend fun registrar(nombre: String, correo: String, contrasena: String): Result<Usuario> =
        ejecutarSeguro {
            val uid = auth.createUserWithEmailAndPassword(correo, contrasena).await().user?.uid
                ?: throw AuthError.Desconocido
            db.collection(COLECCION_USUARIOS).document(uid).set(
                mapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "rol" to Rol.POSTULANTE.valorFirestore
                )
            ).await()
            Usuario(uid, nombre, correo, Rol.POSTULANTE)
        }

    override fun haySesionActiva(): Boolean = auth.currentUser != null

    override fun cerrarSesion() = auth.signOut()

    override fun observarUsuarioActual(): Flow<Usuario?> = callbackFlow {
        var registroUsuario: ListenerRegistration? = null

        // 1) Escucha los inicios y cierres de sesión
        val escuchaSesion = FirebaseAuth.AuthStateListener { firebaseAuth ->
            registroUsuario?.remove()
            registroUsuario = null
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
                return@AuthStateListener
            }
            // 2) Con sesión, escucha el documento del usuario (ahí está su rol)
            registroUsuario = db.collection(COLECCION_USUARIOS).document(uid)
                .addSnapshotListener { doc, error ->
                    if (error != null) return@addSnapshotListener // sin red: se queda el último valor
                    val rol = Rol.desdeFirestore(doc?.getString("rol"))
                    if (doc == null || !doc.exists() || rol == null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(
                        Usuario(
                            uid = uid,
                            nombre = doc.getString("nombre").orEmpty(),
                            correo = doc.getString("correo").orEmpty(),
                            rol = rol
                        )
                    )
                }
        }

        auth.addAuthStateListener(escuchaSesion)
        awaitClose {
            auth.removeAuthStateListener(escuchaSesion)
            registroUsuario?.remove()
        }
    }

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
        is FirebaseAuthWeakPasswordException -> AuthError.ContrasenaDebil
        is FirebaseAuthUserCollisionException -> AuthError.CorreoEnUso
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