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
- Ícono de la app y logo "Marco con pigmentos" en vectores, con versión monocromática para Android 13

### HU04: Resolución de observaciones y aprobación de membresía
- Bandeja del Anfitrión con pestañas Observadas y Verificadas, en tiempo real
- Observadas: corregir y aprobar (con nota), pedir más información o rechazar (con motivo)
- Verificadas: aprobar o rechazar (con motivo)
- Cloud Function `resolverPostulacion`: valida el rol, crea el Socio Activo con su número (SOC-0001) en una transacción y cambia el rol en Firestore
- Pruebas unitarias del caso de uso `ResolverPostulacionUseCase`

### HU05: Perfil offline del Socio Activo
- Ficha del socio (número, estado de membresía, fecha de alta y datos) guardada en SQLite con Room
- El Perfil lee siempre de Room: funciona sin internet y muestra "Sin conexión · mostrando datos guardados"
- Sincronización con WorkManager al recuperar la conexión, cada 6 horas y al deslizar hacia abajo
- Ingreso automático con huella si hay sesión abierta (también sin internet)
- Al cerrar sesión se borra la ficha del celular

## Sprint 3 — Registro de obras y asignación de expertos

### HU06: Registro de obras con evidencia fotográfica
- Registro de una o varias obras (título, técnica, alto, ancho y profundidad opcional) con validación de campos obligatorios
- Foto obligatoria tomada con la cámara del sistema o elegida de la galería, reducida a 1600 px y girada según el EXIF
- Nota de voz opcional transcrita a texto con SpeechRecognizer; el texto se corrige o se escribe a mano
- Borrador de la solicitud de exhibición guardado en SQLite con Room (migración a la versión 2): funciona sin internet
- Al enviar la solicitud, WorkManager sube las fotos a Firebase Storage y la solicitud con sus obras a Firestore en un batch, apenas hay conexión
- Reglas de Firestore y de Storage: solo el Socio Activo crea sus solicitudes y sube sus fotos
- Pruebas unitarias del formulario y del envío

## Tecnologías
Kotlin, Jetpack Compose, Material 3, Navigation Compose, Firebase Auth, Cloud Firestore, Cloud Functions (Node.js), Firebase Storage, Retrofit, corrutinas, WorkManager, Room (SQLite), DataStore, BiometricPrompt, SpeechRecognizer, FileProvider

## Cómo ejecutar
1. Clonar el repositorio y abrirlo en Android Studio.
2. Crear `local.properties` con `DNI_API_MOCK=true` (o `false` y `DNI_API_TOKEN=...` para la API real). Este archivo no se sube.
3. Ejecutar en un celular con Android 7.0 o superior.

## Cloud Functions y reglas
1. Instalar Node.js y `npm install -g firebase-tools`; luego `firebase login`.
2. `cd functions` y `npm install`.
3. Desde la raíz: `firebase deploy --only "functions,firestore:rules,storage"` (requiere el plan Blaze).

## Equipo
- Jefferson Rojas (jeffersonr-arch)
- Andre Valenzuela (alonsov4l)
