/**
 * TECNISIS Galería · Cloud Functions
 * HU04: el Anfitrión resuelve una postulación (aprobar, rechazar o pedir más información).
 * Corre con permisos de administrador: puede escribir en socios, contadores y usuarios,
 * que las reglas de Firestore bloquean para la app.
 */
const {setGlobalOptions} = require("firebase-functions");
const {onCall, HttpsError} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

// Misma región que usa la app: FirebaseFunctions.getInstance("southamerica-east1")
setGlobalOptions({region: "southamerica-east1", maxInstances: 10});

// Qué decisiones admite cada estado (igual que decisionesPermitidas() en la app)
const PERMITIDAS = {
  observada: ["aprobar", "rechazar", "pedir_informacion"],
  verificada: ["aprobar", "rechazar"],
};

// Rechazar y pedir información siempre llevan texto; aprobar una observada exige la nota de lo corregido
function requiereMensaje(decision, estado) {
  return decision !== "aprobar" || estado === "observada";
}

// 1 -> "SOC-0001", 27 -> "SOC-0027"
function formatearNumeroSocio(n) {
  return "SOC-" + String(n).padStart(4, "0");
}

exports.resolverPostulacion = onCall(async (request) => {
  // 1) Debe haber sesión y el que llama debe ser Anfitrión
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión");
  }
  const anfitrionUid = request.auth.uid;
  const anfitrion = await db.doc(`usuarios/${anfitrionUid}`).get();
  if (!anfitrion.exists || anfitrion.get("rol") !== "anfitrion") {
    throw new HttpsError("permission-denied", "Solo el Anfitrión puede resolver postulaciones");
  }

  // 2) Datos que manda la app: {uid, decision, mensaje}
  const datos = request.data || {};
  const uid = typeof datos.uid === "string" ? datos.uid : "";
  const decision = datos.decision;
  const mensaje = typeof datos.mensaje === "string" ? datos.mensaje.trim() : "";
  if (uid === "") {
    throw new HttpsError("invalid-argument", "Falta el uid de la postulación");
  }
  if (!["aprobar", "rechazar", "pedir_informacion"].includes(decision)) {
    throw new HttpsError("invalid-argument", "Decisión no válida");
  }

  const refPostulacion = db.doc(`postulaciones/${uid}`);
  const refUsuario = db.doc(`usuarios/${uid}`);
  const refSocio = db.doc(`socios/${uid}`);
  const refContador = db.doc("contadores/socios");

  // 3) Todo en una transacción: o se guarda todo o nada (y dos aprobaciones a la vez no repiten número)
  const resultado = await db.runTransaction(async (tx) => {
    // En una transacción primero se lee todo y después se escribe
    const postulacion = await tx.get(refPostulacion);
    if (!postulacion.exists) {
      throw new HttpsError("not-found", "La postulación no existe");
    }
    const estado = postulacion.get("estado");
    if (!(PERMITIDAS[estado] || []).includes(decision)) {
      throw new HttpsError("failed-precondition", `Una postulación ${estado} no admite "${decision}"`);
    }
    if (mensaje === "" && requiereMensaje(decision, estado)) {
      throw new HttpsError("invalid-argument", "El motivo o mensaje es obligatorio");
    }

    if (decision === "pedir_informacion") {
      // Sigue observada; el postulante verá el mensaje en su pantalla de estado
      tx.update(refPostulacion, {
        mensajeAnfitrion: mensaje,
        fechaMensaje: FieldValue.serverTimestamp(),
      });
      return {estado};
    }

    if (decision === "rechazar") {
      tx.update(refPostulacion, {
        estado: "rechazada",
        motivoRechazo: mensaje,
        resueltaPor: anfitrionUid,
        fechaResolucion: FieldValue.serverTimestamp(),
      });
      return {estado: "rechazada"};
    }

    // decision === "aprobar"
    const usuario = await tx.get(refUsuario);
    if (!usuario.exists) {
      throw new HttpsError("not-found", "El postulante no tiene cuenta en usuarios");
    }
    const contador = await tx.get(refContador);
    const siguiente = Number(contador.exists ? contador.get("ultimo") || 0 : 0) + 1;
    const numeroSocio = formatearNumeroSocio(siguiente);
    const p = postulacion.data();
    const nombreCompleto = `${p.nombres || ""} ${p.apellidoPaterno || ""} ${p.apellidoMaterno || ""}`
        .replace(/\s+/g, " ")
        .trim();

    tx.set(refContador, {ultimo: siguiente});
    tx.set(refSocio, {
      uid,
      numeroSocio,
      dni: p.dni || "",
      nombreCompleto,
      direccion: p.direccion || "",
      sexo: p.sexo || "",
      telefono: p.telefono || "",
      correo: p.correo || "",
      estadoMembresia: "activa",
      fechaAlta: FieldValue.serverTimestamp(),
      aprobadoPor: anfitrionUid,
    });
    tx.update(refPostulacion, {
      estado: "aprobada",
      notaAprobacion: mensaje === "" ? null : mensaje,
      numeroSocio,
      resueltaPor: anfitrionUid,
      fechaResolucion: FieldValue.serverTimestamp(),
    });
    // Al cambiar el rol, la app del postulante salta sola al inicio de Socio Activo (HU14)
    tx.update(refUsuario, {rol: "socio_activo"});
    return {estado: "aprobada", numeroSocio};
  });

  logger.info("Postulación resuelta", {uid, decision, por: anfitrionUid, ...resultado});
  return resultado;
});
