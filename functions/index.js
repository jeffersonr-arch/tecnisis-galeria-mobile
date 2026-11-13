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

// =====================================================================================
// HU07 · Análisis con IA de una solicitud de exhibición (función HTTP; la app la llama con Retrofit)
//   1) Gemini Flash describe cada obra con su texto y su foto.
//   2) gemini-embedding-2 convierte cada obra y cada perfil de Experto en un vector de 768 números.
//   3) Por obra: solo compiten los Expertos que dominan su técnica y están disponibles
//      (activo y carga < 5, contando las obras de esta misma solicitud). Gana la mayor similitud
//      coseno; a igual afinidad (en %), la menor carga.
// La API key de Gemini es un secreto de Cloud Functions: nunca está en la app ni en el repositorio.
// =====================================================================================
const {onRequest} = require("firebase-functions/https");
const {defineSecret} = require("firebase-functions/params");
const {getAuth} = require("firebase-admin/auth");
const {getStorage} = require("firebase-admin/storage");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/models";
const MODELO_TEXTO = "gemini-3.6-flash"; // versión estable, escrita exacta (sin -latest)
const MODELO_EMBEDDING = "gemini-embedding-2";
const DIMENSIONES = 768;
const CARGA_MAXIMA = 5; // igual que CARGA_MAXIMA en la app
const BLOQUEO_MS = 3 * 60 * 1000; // un análisis "en_curso" de más de 3 minutos se da por abandonado

const ETIQUETA_TECNICA = {
  oleo: "óleo", acrilico: "acrílico", acuarela: "acuarela", tecnica_mixta: "técnica mixta",
  dibujo: "dibujo", grabado: "grabado", escultura: "escultura", fotografia: "fotografía",
  ceramica: "cerámica", textil: "textil",
};

/** POST a la Gemini API. Si responde con error, lanza una excepción con el detalle. */
async function llamarGemini(modelo, metodo, cuerpo, clave) {
  const respuesta = await fetch(`${URL_GEMINI}/${modelo}:${metodo}`, {
    method: "POST",
    headers: {"Content-Type": "application/json", "x-goog-api-key": clave},
    body: JSON.stringify(cuerpo),
    signal: AbortSignal.timeout(45000),
  });
  const datos = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    const detalle = (datos.error && datos.error.message) || "sin detalle";
    throw new Error(`Gemini ${modelo} respondió ${respuesta.status}: ${detalle}`);
  }
  return datos;
}

function medidas(obra) {
  const partes = [obra.altoCm, obra.anchoCm];
  if (typeof obra.profundidadCm === "number") partes.push(obra.profundidadCm);
  return partes.join(" × ") + " cm";
}

/** Gemini Flash: 2 o 3 oraciones sobre la obra, a partir de la foto y de los datos del artista. */
async function describir(obra, foto, clave) {
  const instrucciones = [
    "Eres curador de una galería de arte en Lima.",
    "Describe en español la obra de la foto en 2 o 3 oraciones (máximo 70 palabras).",
    "Menciona lo que se ve, la paleta de colores y la técnica aparente. No inventes el nombre del autor.",
    `Datos del artista: título "${obra.titulo}", técnica ${ETIQUETA_TECNICA[obra.tecnica] || obra.tecnica}, ` +
      `${medidas(obra)}.`,
    obra.nota ? `Nota del artista: ${obra.nota}` : "",
    "Responde solo con la descripción, sin títulos ni viñetas.",
  ].filter(Boolean).join("\n");

  const datos = await llamarGemini(MODELO_TEXTO, "generateContent", {
    contents: [{
      role: "user",
      parts: [
        {text: instrucciones},
        {inline_data: {mime_type: "image/jpeg", data: foto.toString("base64")}},
      ],
    }],
  }, clave);

  const partes = (((datos.candidates || [])[0] || {}).content || {}).parts || [];
  const texto = partes.filter((p) => p.text && !p.thought).map((p) => p.text).join(" ").trim();
  if (!texto) throw new Error(`Gemini no devolvió texto para la obra ${obra.id}`);
  return texto.slice(0, 800);
}

/** gemini-embedding-2 no acepta task_type: la tarea va dentro del texto, igual para obras y Expertos. */
async function embeber(texto, clave) {
  const datos = await llamarGemini(MODELO_EMBEDDING, "embedContent", {
    content: {parts: [{text: `task: sentence similarity | query: ${texto}`}]},
    output_dimensionality: DIMENSIONES,
  }, clave);
  const valores = (datos.embedding && datos.embedding.values) ||
    (datos.embeddings && datos.embeddings[0] && datos.embeddings[0].values);
  if (!Array.isArray(valores) || valores.length === 0) throw new Error("Gemini devolvió un embedding vacío");
  return valores;
}

/**
 * Lee el vector guardado; si no existe o cambió el texto (o el modelo), lo vuelve a calcular y lo guarda
 * junto al texto usado. Así, si alguien edita el perfil de un Experto, se recalcula solo.
 */
async function vectorGuardado(ref, texto, clave) {
  const doc = await ref.get();
  if (doc.exists && doc.get("texto") === texto && doc.get("modelo") === MODELO_EMBEDDING &&
      Array.isArray(doc.get("vector"))) {
    return doc.get("vector");
  }
  const vector = await embeber(texto, clave);
  await ref.set({texto, vector, modelo: MODELO_EMBEDDING, dimensiones: vector.length,
    fecha: FieldValue.serverTimestamp()});
  return vector;
}

/** Similitud coseno: 1 = mismo sentido, 0 = nada en común. Divide entre las longitudes (no hace falta normalizar). */
function coseno(a, b) {
  let punto = 0;
  let normaA = 0;
  let normaB = 0;
  for (let i = 0; i < Math.min(a.length, b.length); i++) {
    punto += a[i] * b[i];
    normaA += a[i] * a[i];
    normaB += b[i] * b[i];
  }
  return normaA === 0 || normaB === 0 ? 0 : punto / (Math.sqrt(normaA) * Math.sqrt(normaB));
}

function textoObra(obra) {
  return [
    `Obra de ${ETIQUETA_TECNICA[obra.tecnica] || obra.tecnica}.`,
    `Título: ${obra.titulo}.`,
    obra.nota ? `Nota del artista: ${obra.nota}` : "",
    obra.descripcionIa || "",
  ].filter(Boolean).join(" ");
}

function textoExperto(e) {
  const tecnicas = (e.tecnicas || []).map((t) => ETIQUETA_TECNICA[t] || t).join(", ");
  return `Especialista de arte en ${tecnicas}. ${e.perfil || ""}`.trim();
}

/** Todo el trabajo con Gemini. Cada resultado se guarda apenas está listo: un reintento hace solo lo que falta. */
async function procesarSolicitud(solicitudId, clave) {
  const obrasSnap = await db.collection("obras").where("solicitudId", "==", solicitudId).get();
  const obras = obrasSnap.docs
      .map((d) => ({id: d.id, ref: d.ref, ...d.data()}))
      .sort((a, b) => (a.fechaRegistro ? a.fechaRegistro.toMillis() : 0) -
        (b.fechaRegistro ? b.fechaRegistro.toMillis() : 0));
  if (obras.length === 0) throw new Error("La solicitud no tiene obras");

  // a) Descripción de cada obra (solo las que aún no la tienen)
  const bucket = getStorage().bucket();
  for (const obra of obras) {
    if (obra.descripcionIa) continue;
    const [foto] = await bucket.file(obra.fotoRuta).download();
    obra.descripcionIa = await describir(obra, foto, clave);
    await obra.ref.update({descripcionIa: obra.descripcionIa});
  }

  // b) Vectores de las obras y de los Expertos
  for (const obra of obras) {
    obra.vector = await vectorGuardado(db.doc(`obras_ia/${obra.id}`), textoObra(obra), clave);
  }
  const expertos = [];
  for (const d of (await db.collection("expertos").get()).docs) {
    const e = {uid: d.id, ...d.data()};
    e.carga = Number(e.carga) || 0;
    e.vector = await vectorGuardado(db.doc(`expertos_ia/${e.uid}`), textoExperto(e), clave);
    expertos.push(e);
  }

  // c) Recomendación por obra (en orden de registro)
  const extra = {}; // uid → obras de ESTA solicitud ya recomendadas a ese Experto
  let obrasSinExperto = 0;
  for (const obra of obras) {
    const candidatos = expertos
        .filter((e) => (e.tecnicas || []).includes(obra.tecnica))
        .map((e) => ({uid: e.uid, afinidad: Math.round(Math.max(0, coseno(obra.vector, e.vector)) * 100)}))
        .sort((a, b) => b.afinidad - a.afinidad);

    const cargaCon = (uid) => expertos.find((e) => e.uid === uid).carga + (extra[uid] || 0);
    const disponibles = candidatos
        .filter((c) => expertos.find((e) => e.uid === c.uid).estado === "activo" && cargaCon(c.uid) < CARGA_MAXIMA)
        .sort((a, b) => b.afinidad - a.afinidad || cargaCon(a.uid) - cargaCon(b.uid));

    const elegido = disponibles.length > 0 ? disponibles[0].uid : null;
    if (elegido) {
      extra[elegido] = (extra[elegido] || 0) + 1;
    } else {
      obrasSinExperto++;
    }
    await obra.ref.update({candidatos, recomendadoUid: elegido});
  }
  return {obrasSinExperto};
}

exports.analizarSolicitud = onRequest(
    {secrets: [GEMINI_API_KEY], timeoutSeconds: 120, memory: "512MiB"},
    async (req, res) => {
      if (req.method !== "POST") {
        res.status(405).json({error: "Usa POST"});
        return;
      }

      // 1) Es una URL pública: solo pasa un Anfitrión con sesión válida (si no, cualquiera gastaría la cuota)
      const cabecera = req.get("Authorization") || "";
      const token = cabecera.startsWith("Bearer ") ? cabecera.slice(7) : "";
      let uid;
      try {
        uid = (await getAuth().verifyIdToken(token)).uid;
      } catch (error) {
        res.status(401).json({error: "Sesión no válida"});
        return;
      }
      const usuario = await db.doc(`usuarios/${uid}`).get();
      if (!usuario.exists || usuario.get("rol") !== "anfitrion") {
        res.status(403).json({error: "Solo el Anfitrión puede analizar solicitudes"});
        return;
      }
      const solicitudId = req.body && typeof req.body.solicitudId === "string" ? req.body.solicitudId : "";
      if (solicitudId === "") {
        res.status(400).json({error: "Falta solicitudId"});
        return;
      }
      const refSolicitud = db.doc(`solicitudes/${solicitudId}`);

      // 2) Bloqueo en una transacción, con la hora del servidor (Date.now() aquí es la del servidor)
      let respuestaTemprana;
      try {
        respuestaTemprana = await db.runTransaction(async (tx) => {
          const s = await tx.get(refSolicitud);
          if (!s.exists) return {codigo: 404, cuerpo: {error: "La solicitud no existe"}};
          if (s.get("estado") !== "enviada") return {codigo: 409, cuerpo: {error: "La solicitud ya fue resuelta"}};
          const analisis = s.get("analisis");
          if (analisis === "listo") {
            return {codigo: 200, cuerpo: {analisis: "listo", obrasSinExperto: s.get("obrasSinExperto") || 0}};
          }
          const desde = s.get("analisisDesde");
          if (analisis === "en_curso" && desde && Date.now() - desde.toMillis() < BLOQUEO_MS) {
            return {codigo: 202, cuerpo: {analisis: "en_curso"}}; // otra llamada lo está haciendo
          }
          tx.update(refSolicitud, {
            analisis: "en_curso",
            analisisDesde: FieldValue.serverTimestamp(),
            analisisError: null,
          });
          return null; // esta llamada hace el análisis
        });
      } catch (error) {
        logger.error("No se pudo bloquear la solicitud", {solicitudId, error: error.message});
        res.status(500).json({error: "No se pudo iniciar el análisis"});
        return;
      }
      if (respuestaTemprana) {
        res.status(respuestaTemprana.codigo).json(respuestaTemprana.cuerpo);
        return;
      }

      // 3) Análisis. Si falla, queda "error" y lo ya guardado se conserva para el reintento.
      try {
        const {obrasSinExperto} = await procesarSolicitud(solicitudId, GEMINI_API_KEY.value());
        await refSolicitud.update({
          analisis: "listo",
          analisisFin: FieldValue.serverTimestamp(),
          obrasSinExperto,
        });
        logger.info("Solicitud analizada", {solicitudId, obrasSinExperto, por: uid});
        res.status(200).json({analisis: "listo", obrasSinExperto});
      } catch (error) {
        logger.error("Falló el análisis", {solicitudId, error: error.message});
        await refSolicitud.update({analisis: "error", analisisError: String(error.message).slice(0, 300)})
            .catch(() => {});
        res.status(502).json({analisis: "error", error: "No se pudo analizar la solicitud"});
      }
    },
);

// =====================================================================================
// HU07 · Parte 2: el Anfitrión aprueba o rechaza la solicitud (callable, como resolverPostulacion)
//   1) La app ya subió los 2 PDF: aquí se revisa que existan antes de cambiar nada.
//   2) En una transacción: revalida cada Experto (domina la técnica, activo y con cupo),
//      cambia la solicitud y sus obras, y suma la carga de cada Experto.
//   3) Avisa al socio con Firebase Cloud Messaging, si tiene token.
// =====================================================================================
const {getMessaging} = require("firebase-admin/messaging");

const MOTIVO_MAXIMO = 500; // igual que MOTIVO_MAXIMO en la app
const COPIAS = ["archivo.pdf", "artista.pdf"];

/** La app lee details.motivo para saber qué condición falló. */
function noCumple(mensaje, motivo) {
  return new HttpsError("failed-precondition", mensaje, {motivo});
}

exports.decidirSolicitud = onCall(async (request) => {
  // 1) Sesión y rol
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión");
  }
  const anfitrionUid = request.auth.uid;
  const anfitrion = await db.doc(`usuarios/${anfitrionUid}`).get();
  if (!anfitrion.exists || anfitrion.get("rol") !== "anfitrion") {
    throw new HttpsError("permission-denied", "Solo el Anfitrión puede aprobar o rechazar solicitudes");
  }

  // 2) Datos que manda la app: {solicitudId, decision, asignaciones, motivo}
  const datos = request.data || {};
  const solicitudId = typeof datos.solicitudId === "string" ? datos.solicitudId : "";
  const decision = datos.decision;
  const motivo = typeof datos.motivo === "string" ? datos.motivo.trim() : "";
  const asignaciones = datos.asignaciones && typeof datos.asignaciones === "object" ? datos.asignaciones : {};
  if (solicitudId === "") {
    throw new HttpsError("invalid-argument", "Falta el id de la solicitud");
  }
  if (!["aprobar", "rechazar"].includes(decision)) {
    throw new HttpsError("invalid-argument", "Decisión no válida");
  }
  if (decision === "rechazar" && (motivo.length < 1 || motivo.length > MOTIVO_MAXIMO)) {
    throw new HttpsError("invalid-argument", "El motivo es obligatorio (1 a 500 caracteres)");
  }

  // 3) Los 2 PDF deben estar en Storage antes de cambiar nada
  const bucket = getStorage().bucket();
  for (const copia of COPIAS) {
    const [existe] = await bucket.file(`documentos/${solicitudId}/${copia}`).exists();
    if (!existe) {
      throw noCumple("Faltan los documentos de la solicitud", "faltan_documentos");
    }
  }

  // 4) Todo junto o nada: solicitud, obras y carga de los Expertos
  const refSolicitud = db.doc(`solicitudes/${solicitudId}`);
  const resultado = await db.runTransaction(async (tx) => {
    // En una transacción, todas las lecturas van antes de las escrituras
    const s = await tx.get(refSolicitud);
    if (!s.exists) {
      throw new HttpsError("not-found", "La solicitud no existe");
    }
    if (s.get("estado") !== "enviada") {
      throw noCumple("La solicitud ya fue resuelta", "ya_resuelta");
    }
    const obras = (await tx.get(db.collection("obras").where("solicitudId", "==", solicitudId))).docs;
    if (obras.length === 0) {
      throw new HttpsError("not-found", "La solicitud no tiene obras");
    }
    const comun = {
      resueltaPor: anfitrionUid,
      fechaResolucion: FieldValue.serverTimestamp(),
      documentos: COPIAS.map((c) => `documentos/${solicitudId}/${c}`),
    };

    if (decision === "rechazar") {
      tx.update(refSolicitud, {...comun, estado: "rechazada", motivoRechazo: motivo});
      obras.forEach((o) => tx.update(o.ref, {estado: "no_admitida"}));
      return {estado: "rechazada", uidSocio: s.get("uid"), cantidad: obras.length};
    }

    // Aprobar: cada obra con un Experto, y cuántas obras de esta solicitud recibe cada uno
    const porExperto = {};
    for (const o of obras) {
      const uid = asignaciones[o.id];
      if (typeof uid !== "string" || uid === "") {
        throw noCumple(`La obra "${o.get("titulo")}" no tiene Experto`, "experto_no_disponible");
      }
      porExperto[uid] = (porExperto[uid] || 0) + 1;
    }
    const expertos = {};
    for (const uid of Object.keys(porExperto)) {
      const e = await tx.get(db.doc(`expertos/${uid}`));
      if (!e.exists) {
        throw noCumple("Uno de los Expertos elegidos ya no existe", "experto_no_disponible");
      }
      expertos[uid] = e;
    }
    // Misma regla que la app: domina la técnica, está activo y su carga más estas obras no pasa de 5
    for (const o of obras) {
      const e = expertos[asignaciones[o.id]];
      const domina = (e.get("tecnicas") || []).includes(o.get("tecnica"));
      const conCupo = (Number(e.get("carga")) || 0) + porExperto[e.id] <= CARGA_MAXIMA;
      if (!domina || e.get("estado") !== "activo" || !conCupo) {
        throw noCumple(`${e.get("nombre")} ya no está disponible para "${o.get("titulo")}"`, "experto_no_disponible");
      }
    }
    // HU08: el Especialista de Arte no lee la ficha del socio; se copian en cada obra solo su nombre y su número
    const socio = await tx.get(db.doc(`socios/${s.get("uid")}`));
    const nombreSocio = socio.exists ? (socio.get("nombreCompleto") || null) : null;
    const numeroSocio = socio.exists ? (socio.get("numeroSocio") || null) : null;
    tx.update(refSolicitud, {...comun, estado: "aprobada"});
    obras.forEach((o) => tx.update(o.ref, {
      estado: "en_evaluacion",
      expertoUid: asignaciones[o.id],
      fechaAsignacion: FieldValue.serverTimestamp(),
      nombreSocio,
      numeroSocio,
    }));
    for (const [uid, n] of Object.entries(porExperto)) {
      tx.update(db.doc(`expertos/${uid}`), {carga: FieldValue.increment(n)});
    }
    return {estado: "aprobada", uidSocio: s.get("uid"), cantidad: obras.length};
  });

  // 5) Aviso al socio. Si falla, la decisión ya quedó guardada
  await avisarSocio(resultado.uidSocio, solicitudId, resultado.estado, resultado.cantidad);
  logger.info("Solicitud resuelta", {solicitudId, estado: resultado.estado, por: anfitrionUid});
  return {estado: resultado.estado};
});

/** Mensaje de datos (no de notificación): la app arma el aviso y baja la decisión a Room. */
async function avisarSocio(uid, solicitudId, estado, cantidad) {
  await enviarAviso(uid, {tipo: "decision", solicitudId, estado, cantidad: String(cantidad)});
}

/**
 * HU07 y HU08: manda un mensaje de datos de FCM al token del socio (tokens/{uid}).
 * En FCM, todos los valores de "data" deben ser texto. Si falla, solo se registra: lo que se decidió ya quedó guardado.
 */
async function enviarAviso(uid, datos) {
  try {
    const doc = await db.doc(`tokens/${uid}`).get();
    const token = doc.exists ? doc.get("token") : null;
    if (!token) {
      logger.info("El socio no tiene token de avisos", {uid});
      return;
    }
    await getMessaging().send({
      token,
      data: datos,
      android: {priority: "high"},
    });
  } catch (error) {
    logger.warn("No se pudo avisar al socio", {uid, error: error.message});
    // Token vencido (cerró sesión o desinstaló la app): se borra para no volver a intentarlo
    if (error.code === "messaging/registration-token-not-registered" ||
        error.code === "messaging/invalid-registration-token") {
      await db.doc(`tokens/${uid}`).delete().catch(() => {});
    }
  }
}

// =====================================================================================
// HU08: el Especialista de Arte registra el veredicto de una obra (callable, como decidirSolicitud)
//   1) Sesión, rol especialista_arte y datos: {obraId, veredicto: "aceptar"|"rechazar", comentario}.
//   2) En una transacción: que la obra sea suya (expertoUid), que siga "en_evaluacion" y sin veredicto;
//      guarda el veredicto y baja su carga (lee la carga y guarda max(0, carga - 1): nunca queda negativa).
//   3) Avisa al socio con FCM (mensaje de datos tipo "veredicto").
// =====================================================================================
const COMENTARIO_MAXIMO = 500; // igual que COMENTARIO_MAXIMO en la app
const VEREDICTOS = {aceptar: "aceptada", rechazar: "rechazada"};

exports.registrarVeredicto = onCall(async (request) => {
  // 1) Sesión y rol
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión");
  }
  const uid = request.auth.uid;
  const usuario = await db.doc(`usuarios/${uid}`).get();
  if (!usuario.exists || usuario.get("rol") !== "especialista_arte") {
    throw new HttpsError("permission-denied", "Solo el Especialista de Arte puede registrar veredictos");
  }

  // Datos que manda la app
  const datos = request.data || {};
  const obraId = typeof datos.obraId === "string" ? datos.obraId : "";
  const estadoNuevo = VEREDICTOS[datos.veredicto];
  const comentario = typeof datos.comentario === "string" ? datos.comentario.trim() : "";
  if (obraId === "") {
    throw new HttpsError("invalid-argument", "Falta el id de la obra");
  }
  if (!estadoNuevo) {
    throw new HttpsError("invalid-argument", "Veredicto no válido");
  }
  if (comentario.length > COMENTARIO_MAXIMO || (estadoNuevo === "rechazada" && comentario.length < 1)) {
    throw new HttpsError("invalid-argument", "Al rechazar, el comentario es obligatorio (1 a 500 caracteres)");
  }

  // HU09: al rechazar, los 2 PDF deben estar en Storage antes de cambiar nada
  const rutasRechazo = COPIAS.map((c) => `documentos_obras/${obraId}/${c}`);
  if (estadoNuevo === "rechazada") {
    const bucket = getStorage().bucket();
    for (const ruta of rutasRechazo) {
      const [existe] = await bucket.file(ruta).exists();
      if (!existe) {
        throw noCumple("Faltan los documentos de rechazo de la obra", "faltan_documentos");
      }
    }
  }

  // 2) Todo junto o nada: la obra y la carga del Especialista
  const refObra = db.doc(`obras/${obraId}`);
  const refExperto = db.doc(`expertos/${uid}`);
  const resultado = await db.runTransaction(async (tx) => {
    // En una transacción, todas las lecturas van antes de las escrituras
    const obra = await tx.get(refObra);
    const experto = await tx.get(refExperto);
    if (!obra.exists) {
      throw new HttpsError("not-found", "La obra no existe");
    }
    if (obra.get("expertoUid") !== uid) {
      throw noCumple("Esta obra no está asignada a ti", "no_asignada");
    }
    if (obra.get("estado") !== "en_evaluacion" || obra.get("evaluadaPor")) {
      throw noCumple("La obra ya tiene veredicto", "ya_evaluada");
    }
    tx.update(refObra, {
      estado: estadoNuevo,
      veredictoComentario: comentario === "" ? null : comentario,
      fechaVeredicto: FieldValue.serverTimestamp(),
      evaluadaPor: uid,
      evaluadaPorNombre: experto.exists ? (experto.get("nombre") || null) : null, // HU09
      documentoRechazo: estadoNuevo === "rechazada" ? rutasRechazo : null, // HU09
    });
    if (experto.exists) {
      const carga = Number(experto.get("carga")) || 0;
      tx.update(refExperto, {carga: Math.max(0, carga - 1)});
    }
    return {
      uidSocio: obra.get("uid"),
      solicitudId: obra.get("solicitudId") || "",
      titulo: obra.get("titulo") || "",
    };
  });

  // 3) Aviso al socio. Si falla, el veredicto ya quedó guardado
  await enviarAviso(resultado.uidSocio, {
    tipo: "veredicto",
    obraId,
    solicitudId: resultado.solicitudId,
    estado: estadoNuevo,
    titulo: resultado.titulo,
  });
  logger.info("Veredicto registrado", {obraId, estado: estadoNuevo, por: uid});
  return {estado: estadoNuevo};
});

// =====================================================================================
// HU10: el Especialista de Ventas tasa una obra aceptada (callable, como registrarVeredicto)
//   1) Sesión, rol especialista_ventas y datos: {obraId, pvpCentimos, porcentaje}.
//      Vuelve a calcular los montos aquí: no confía en lo que calcula la app.
//   2) Los 2 PDF del documento de venta deben estar en Storage antes de cambiar nada.
//   3) En una transacción: que la obra siga "aceptada"; crea documentos_venta/{obraId} y la pasa a "lista_exhibicion".
//   4) Avisa al socio con FCM (mensaje de datos tipo "tasacion").
// =====================================================================================
const PVP_MAXIMO_CENTIMOS = 100000000; // S/ 1,000,000.00, igual que PVP_MAXIMO_CENTIMOS en la app
const PORCENTAJE_MAXIMO = 100;

// galería = PVP × % ÷ 100 redondeado a 2 decimales (0,005 sube); socio = PVP − galería. Igual que calcularMontos
function calcularMontos(pvpCentimos, porcentaje) {
  const galeria = Math.floor((pvpCentimos * porcentaje + 50) / 100);
  return {montoGaleriaCentimos: galeria, montoSocioCentimos: pvpCentimos - galeria};
}

exports.registrarTasacion = onCall(async (request) => {
  // 1) Sesión y rol
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión");
  }
  const uid = request.auth.uid;
  const usuario = await db.doc(`usuarios/${uid}`).get();
  if (!usuario.exists || usuario.get("rol") !== "especialista_ventas") {
    throw new HttpsError("permission-denied", "Solo el Especialista de Ventas puede tasar obras");
  }

  // Datos que manda la app
  const datos = request.data || {};
  const obraId = typeof datos.obraId === "string" ? datos.obraId : "";
  const pvpCentimos = Number(datos.pvpCentimos);
  const porcentaje = Number(datos.porcentaje);
  if (obraId === "") {
    throw new HttpsError("invalid-argument", "Falta el id de la obra");
  }
  if (!Number.isInteger(pvpCentimos) || pvpCentimos < 1 || pvpCentimos > PVP_MAXIMO_CENTIMOS) {
    throw new HttpsError("invalid-argument", "El PVP debe ser mayor a 0 y como máximo S/ 1,000,000.00");
  }
  if (!Number.isInteger(porcentaje) || porcentaje < 0 || porcentaje > PORCENTAJE_MAXIMO) {
    throw new HttpsError("invalid-argument", "El porcentaje debe ser un entero de 0 a 100");
  }
  const montos = calcularMontos(pvpCentimos, porcentaje);

  // 2) Los 2 PDF deben estar en Storage antes de cambiar nada
  const bucket = getStorage().bucket();
  for (const copia of COPIAS) {
    const [existe] = await bucket.file(`documentos_venta/${obraId}/${copia}`).exists();
    if (!existe) {
      throw noCumple("Faltan los documentos de venta de la obra", "faltan_documentos");
    }
  }

  // 3) Todo junto o nada: el documento de venta y el estado de la obra
  const refObra = db.doc(`obras/${obraId}`);
  const refVenta = db.doc(`documentos_venta/${obraId}`);
  const nombre = usuario.get("nombre") || "";
  const resultado = await db.runTransaction(async (tx) => {
    const obra = await tx.get(refObra);
    if (!obra.exists) {
      throw new HttpsError("not-found", "La obra no existe");
    }
    if (obra.get("estado") !== "aceptada") {
      throw noCumple("La obra ya no está por tasar", "ya_tasada");
    }
    tx.set(refVenta, {
      obraId,
      solicitudId: obra.get("solicitudId") || "",
      uid: obra.get("uid") || "",
      nombreSocio: obra.get("nombreSocio") || null,
      numeroSocio: obra.get("numeroSocio") || null,
      titulo: obra.get("titulo") || "",
      tecnica: obra.get("tecnica") || "",
      fotoRuta: obra.get("fotoRuta") || "",
      pvpCentimos,
      porcentaje,
      montoGaleriaCentimos: montos.montoGaleriaCentimos,
      montoSocioCentimos: montos.montoSocioCentimos,
      tasadaPor: uid,
      tasadaPorNombre: nombre === "" ? "Especialista de Ventas" : nombre,
      fechaTasacion: FieldValue.serverTimestamp(),
    });
    tx.update(refObra, {
      estado: "lista_exhibicion",
      fechaTasacion: FieldValue.serverTimestamp(),
      tasadaPor: uid,
    });
    return {
      uidSocio: obra.get("uid"),
      solicitudId: obra.get("solicitudId") || "",
      titulo: obra.get("titulo") || "",
    };
  });

  // 4) Aviso al socio. Si falla, la tasación ya quedó guardada
  await enviarAviso(resultado.uidSocio, {
    tipo: "tasacion",
    obraId,
    solicitudId: resultado.solicitudId,
    estado: "lista_exhibicion",
    titulo: resultado.titulo,
  });
  logger.info("Tasación registrada", {obraId, pvpCentimos, porcentaje, por: uid});
  return {pvpCentimos, porcentaje, ...montos};
});
