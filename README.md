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

### HU07 (Parte 1): Recomendación de Experto con IA
- Pestaña Solicitudes del Anfitrión en tiempo real (por revisar y resueltas), con el detalle de cada solicitud
- Función HTTP `analizarSolicitud` llamada desde la app con Retrofit y corrutinas; verifica la sesión y el rol Anfitrión
- IA generativa: Gemini Flash (`gemini-3.6-flash`) describe cada obra a partir de su foto y sus datos
- Recomendación basada en contenido: embeddings `gemini-embedding-2` (768 dimensiones) y similitud coseno; solo compiten los Expertos que dominan la técnica y están disponibles; a igual afinidad, el de menor carga
- El Anfitrión puede cambiar el Experto recomendado por otro disponible
- La API key de Gemini es un secreto de Cloud Functions: nunca está en la app ni en el repositorio
- Pruebas unitarias de las reglas de disponibilidad y selección de Expertos

### HU07 (Parte 2): Aprobación o rechazo con documento por duplicado
- El Anfitrión aprueba la solicitud solo si todas las obras tienen Experto; puede rechazarla siempre, con motivo obligatorio (hasta 500 caracteres)
- Documento por duplicado generado en el celular con PdfDocument (copia del archivo de la galería y copia del artista), guardado en Firebase Storage
- Cloud Function `decidirSolicitud`: revisa que existan los 2 PDF, vuelve a validar cada Experto y, en una transacción, cambia la solicitud y sus obras y suma la carga de cada Experto
- Notificación al socio con Firebase Cloud Messaging; al tocarla se abre Mis obras
- Sincronización Firestore → Room con WorkManager: Mis obras muestra Aprobada o Rechazada (con el motivo) y la copia del artista; si el socio cambió de celular, recupera su solicitud
- Reglas de Storage: el socio solo descarga su copia y solo cuando la solicitud ya está resuelta
- Pruebas unitarias de la decisión, del documento y del aviso

## Sprint 4 — Evaluación artística

### HU08: Revisión física y veredicto (ML Kit)
- Pestaña Asignadas del Especialista de Arte en tiempo real (Por evaluar y Evaluadas) con su carga de trabajo
- Detalle de la obra con foto, datos, descripción IA y socio (solo nombre y número)
- Google ML Kit Image Labeling en el celular (modelo incluido): hasta 5 etiquetas con 50 % o más, traducidas al español
- Veredicto Aceptada o Rechazada (comentario obligatorio al rechazar), definitivo, con la Cloud Function `registrarVeredicto`
- Notificación al socio por obra (FCM) y veredicto en Mis obras (Room versión 4); el Anfitrión ve el estado de cada obra
- Pruebas unitarias del veredicto, las etiquetas y las listas

### HU09: Documento de rechazo de obra
- Al rechazar, el celular del Especialista genera el documento por duplicado con PdfDocument (copia del archivo de la galería y copia del artista) y lo sube a Firebase Storage antes de guardar el veredicto
- `registrarVeredicto` revisa que existan los 2 PDF y guarda sus rutas en la obra (`documentoRechazo`) y el nombre del Especialista
- El documento se abre desde el detalle del Especialista, Mis obras del socio (su copia), el detalle del Anfitrión y el histórico del Gerente
- Histórico de obras rechazadas del Gerente en tiempo real, con socio, Especialista, fecha, motivo y documento
- Reglas de Storage con `firestore.get()`: escribe el Especialista de la obra; leen el Anfitrión, el Gerente y el Especialista; el socio solo su copia
- Pruebas unitarias del documento y del histórico

## Sprint 5 — Evaluación económica

### HU10: Tasación y márgenes de venta
- Pestañas Por tasar (obras aceptadas, la de veredicto más antiguo primero) y Tasadas del Especialista de Ventas, en tiempo real
- Tasación con PVP (mayor a 0 y hasta S/ 1,000,000.00, coma o punto con 2 decimales) y porcentaje de ganancia de la galería (entero de 0 a 100), con cálculo en vivo del monto neto para el socio; montos en céntimos y redondeo a 2 decimales
- Documento de venta por duplicado generado en el celular con PdfDocument (copia del archivo de la galería y copia del artista) y subido a Firebase Storage antes de guardar
- Cloud Function `registrarTasacion`: valida el rol, recalcula los montos, revisa que existan los 2 PDF y, en una transacción, crea el Documento de Venta (`documentos_venta`) y pasa la obra a "Lista para exhibición"
- Notificación al socio (FCM) y "Mi documento de venta" en Mis obras (su copia)
- Reglas: Ventas lee solo las obras aceptadas y los documentos de venta; el socio solo su copia
- Pruebas unitarias de las reglas de la tasación, del documento y de los casos de uso

## Tecnologías
Kotlin, Jetpack Compose, Material 3, Navigation Compose, Firebase Auth, Cloud Firestore, Cloud Functions (Node.js), Firebase Storage, Retrofit, corrutinas, WorkManager, Room (SQLite), DataStore, BiometricPrompt, SpeechRecognizer, FileProvider, Gemini API (Cloud Functions), Firebase Cloud Messaging, PdfDocument, Google ML Kit Image Labeling

## Cómo ejecutar
1. Clonar el repositorio y abrirlo en Android Studio.
2. Crear `local.properties` con `DNI_API_MOCK=true` (o `false` y `DNI_API_TOKEN=...` para la API real). Este archivo no se sube.
3. Ejecutar en un celular con Android 7.0 o superior.

## Cloud Functions y reglas
1. Instalar Node.js y `npm install -g firebase-tools`; luego `firebase login`.
2. `cd functions` y `npm install`.
3. Guardar la API key de Gemini como secreto: `firebase functions:secrets:set GEMINI_API_KEY`. La key debe estar restringida a la API de Gemini y vinculada a una cuenta de servicio, y el proyecto necesita crédito prepago en Google AI Studio.
4. Desde la raíz: `firebase deploy --only "functions,firestore:rules,storage"` (requiere el plan Blaze).

## Equipo
- Jefferson Rojas (jeffersonr-arch)
- Andre Valenzuela (alonsov4l)
