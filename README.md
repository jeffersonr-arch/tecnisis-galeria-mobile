# TECNISIS Mobile — Galería de Arte
 
App Android de gestión de exposiciones de una galería de arte, con acceso por roles.
 
## Sprint 1 — HU01: Login y control de acceso por rol
- Login con Firebase Authentication (correo y contraseña) y manejo de errores
- Ingreso alterno con huella digital (BiometricPrompt)
- Redirección según el rol guardado en Firestore
- Arquitectura MVVM + Clean (capas data, domain y presentation)
## Sprint 2 — Gestión de membresía
 
### HU02: Postulación y registro de identidad
- Creación de cuenta con rol de postulante y formulario de datos de identidad con validación
- Un DNI solo puede registrarse una vez (documento por DNI y postulación en un mismo batch)
- Notificación de confirmación al enviar la postulación
### HU03: Verificación automática
- Consulta del DNI a una API tipo RENIEC con Retrofit y corrutinas (o una API simulada para pruebas)
- WorkManager con reintentos si no hay internet
- La postulación queda "Verificada" u "Observada" con su motivo, visible en tiempo real
### HU14: Identidad visual y navegación por rol
- Tema "Pigmentos" claro y nocturno, tipografías Fraunces y Manrope, componentes reutilizables
- Barra inferior con las secciones de cada rol; cambio de rol en vivo
- Modo nocturno desde Perfil, guardado con DataStore
### HU04: Resolución de observaciones y aprobación de membresía
- Bandeja del Anfitrión con pestañas Observadas y Verificadas, en tiempo real
- Observadas: corregir y aprobar (con nota), pedir más información o rechazar (con motivo)
- Verificadas: aprobar o rechazar (con motivo)
- Cloud Function `resolverPostulacion`: valida el rol, crea el Socio Activo con su número (SOC-0001) en una transacción y cambia el rol en Firestore
- Pruebas unitarias del caso de uso `ResolverPostulacionUseCase`
## Tecnologías
Kotlin, Jetpack Compose, Material 3, Navigation Compose, Firebase Auth, Cloud Firestore, Cloud Functions (Node.js), Retrofit, corrutinas, WorkManager, DataStore, BiometricPrompt
 
## Cómo ejecutar
1. Clonar el repositorio y abrirlo en Android Studio.
2. Crear `local.properties` con `DNI_API_MOCK=true` (o `false` y `DNI_API_TOKEN=...` para la API real). Este archivo no se sube.
3. Ejecutar en un celular con Android 7.0 o superior.
## Cloud Functions y reglas
1. Instalar Node.js y `npm install -g firebase-tools`; luego `firebase login`.
2. `cd functions` y `npm install`.
3. Desde la raíz: `firebase deploy --only "functions,firestore:rules"` (requiere el plan Blaze).
## Equipo
- Jefferson Rojas (jeffersonr-arch)
- Andre Valenzuela (alonsov4l)
cd functions y npm install.
Desde la raíz: firebase deploy --only "functions,firestore:rules" (requiere el plan Blaze).
Equipo
Jefferson Rojas (jeffersonr-arch)
Andre Valenzuela (alonsov4l)
