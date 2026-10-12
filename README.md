# TECNISIS Mobile — Galería de Arte

App Android de gestión de exposiciones de una galería de arte, con acceso por roles.

## Sprint 1 — HU01: Login y control de acceso por rol
- Login con Firebase Authentication (correo y contraseña) y manejo de errores
- Ingreso alterno con huella digital (BiometricPrompt)
- Redirección según el rol guardado en Firestore
- Arquitectura MVVM + Clean (capas data, domain y presentation)

## Sprint 2 — HU02: Postulación y registro de identidad
- Creación de cuenta desde el login: se registra en Firebase Authentication y en Firestore con el rol de postulante
- Formulario de postulación con datos de identidad (DNI, nombres, apellidos, dirección, sexo, teléfono y correo) y validación de campos
- Un DNI solo puede registrarse una vez: se guarda un documento por DNI junto con la postulación en una sola operación (batch)
- Notificación de confirmación al enviar la postulación (con solicitud de permiso en Android 13 o superior)
- La postulación queda en estado "Pendiente de verificación"; la verificación con RENIEC llega en la HU03
- Reglas de seguridad de Firestore versionadas en `firestore.rules`

## Tecnologías
Kotlin, Jetpack Compose, Material 3, Firebase Auth, Cloud Firestore, Navigation Compose, BiometricPrompt, notificaciones de Android

## Cómo ejecutar
1. Clonar el repositorio y abrirlo en Android Studio.
2. Ejecutar en un celular con Android 7.0 o superior.

## Equipo
- Jefferson Rojas (jeffersonr-arch)
- Andre Valenzuela (alonsov4l)