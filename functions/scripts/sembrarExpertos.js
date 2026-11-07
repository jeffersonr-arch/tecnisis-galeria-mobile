/**
 * TECNISIS Galería · HU07 · Crea o actualiza los 6 Expertos (Especialistas de Arte) de prueba.
 *
 * Se corre UNA vez desde la computadora, nunca desde la app ni desde Cloud Functions.
 * Necesita la clave de cuenta de servicio guardada FUERA del proyecto (C:\Users\Diego\claves\).
 *
 * PowerShell, desde la carpeta functions:
 *   $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\Users\Diego\claves\<archivo>.json"
 *   $env:CLAVE_EXPERTOS = "<contraseña de las cuentas de prueba>"
 *   node scripts/sembrarExpertos.js              crea lo que falte; NO toca carga ni estado existentes
 *   node scripts/sembrarExpertos.js --reiniciar  además vuelve carga y estado a los valores de abajo
 */
const {initializeApp, applicationDefault} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");

initializeApp({credential: applicationDefault(), projectId: "tecnisis-galeria"});
const auth = getAuth();
const db = getFirestore();

const REINICIAR = process.argv.includes("--reiniciar");
const CLAVE = process.env.CLAVE_EXPERTOS || "";

// Las 10 técnicas quedan cubiertas por al menos un Experto activo.
// Pruebas: arte6 inactiva (filtro), arte2 con 4 de 5 (cupo) y Textil solo con arte4 (rechazo).
const EXPERTOS = [
  {
    correo: "arte@tecnisis.pe", nombre: null, // ya existe desde la HU01: se usa su nombre de "usuarios"
    tecnicas: ["oleo", "acrilico", "tecnica_mixta"], estado: "activo", carga: 2,
    perfil: "Especialista en pintura de caballete: óleo, acrílico y técnica mixta. Evalúa paisaje peruano, " +
      "retrato y abstracción contemporánea, con atención al color, la pincelada y el estado del soporte.",
  },
  {
    correo: "arte2@tecnisis.pe", nombre: "Andrés Salazar Ríos",
    tecnicas: ["oleo", "acuarela", "dibujo"], estado: "activo", carga: 4,
    perfil: "Pintor y dibujante académico. Evalúa óleo y acuarela figurativa, bodegón y paisaje costeño, " +
      "y dibujo a lápiz, carboncillo y tinta; se fija en la composición, la perspectiva y el trazo.",
  },
  {
    correo: "arte3@tecnisis.pe", nombre: "Patricia Núñez Vega",
    tecnicas: ["grabado", "dibujo", "fotografia"], estado: "activo", carga: 0,
    perfil: "Especialista en obra gráfica y fotografía. Evalúa grabado en metal, xilografía y serigrafía, " +
      "dibujo, y fotografía documental y de autor; revisa el tiraje, la impresión y el encuadre.",
  },
  {
    correo: "arte4@tecnisis.pe", nombre: "Jorge Huamán Ccori",
    tecnicas: ["textil", "ceramica"], estado: "activo", carga: 1,
    perfil: "Especialista en arte tradicional andino. Evalúa tejidos en telar, tapices y bordados, y cerámica " +
      "de tradición prehispánica; reconoce técnicas, iconografía y tintes naturales.",
  },
  {
    correo: "arte5@tecnisis.pe", nombre: "Lucía Paredes Rojas",
    tecnicas: ["ceramica", "escultura"], estado: "activo", carga: 0,
    perfil: "Escultora y ceramista. Evalúa cerámica utilitaria y artística, esmaltes y quemas, y escultura en " +
      "piedra, madera, metal y resina; revisa el volumen, el acabado y la estabilidad de la pieza.",
  },
  {
    correo: "arte6@tecnisis.pe", nombre: "Elena Torres Ugarte",
    tecnicas: ["escultura", "fotografia", "tecnica_mixta", "acuarela"], estado: "inactivo", carga: 0,
    perfil: "Curadora de arte contemporáneo. Evalúa instalación, escultura, fotografía experimental, acuarela " +
      "y técnica mixta; atiende al concepto de la obra y a su montaje.",
  },
];

/** Devuelve la cuenta de Authentication; si no existe, la crea con la contraseña de $env:CLAVE_EXPERTOS. */
async function cuentaDe(e) {
  try {
    return await auth.getUserByEmail(e.correo);
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
  }
  if (CLAVE.length < 6) {
    throw new Error(`Falta $env:CLAVE_EXPERTOS (mínimo 6 caracteres) para crear ${e.correo}`);
  }
  console.log(`  + Cuenta creada: ${e.correo}`);
  return auth.createUser({email: e.correo, password: CLAVE, displayName: e.nombre});
}

async function sembrar(e) {
  const cuenta = await cuentaDe(e);
  const refUsuario = db.doc(`usuarios/${cuenta.uid}`);
  const usuario = await refUsuario.get();

  if (!usuario.exists) {
    await refUsuario.set({nombre: e.nombre, correo: e.correo, rol: "especialista_arte"});
  } else if (usuario.get("rol") !== "especialista_arte") {
    // Nunca cambia el rol de una cuenta que ya existe: avisa y la salta
    console.log(`  ! ${e.correo} tiene rol "${usuario.get("rol")}": se omite`);
    return null;
  }
  const nombre = e.nombre || usuario.get("nombre") || "Especialista de Arte";

  const refExperto = db.doc(`expertos/${cuenta.uid}`);
  const experto = await refExperto.get();
  const datos = {uid: cuenta.uid, nombre, correo: e.correo, tecnicas: e.tecnicas, perfil: e.perfil};
  if (!experto.exists || REINICIAR) {
    datos.estado = e.estado;
    datos.carga = e.carga;
  }
  if (!experto.exists) datos.fechaAlta = FieldValue.serverTimestamp();
  await refExperto.set(datos, {merge: true});

  const final = (await refExperto.get()).data();
  return {correo: e.correo, nombre, estado: final.estado, carga: final.carga, tecnicas: e.tecnicas.join(", ")};
}

async function main() {
  console.log(REINICIAR ? "Sembrando Expertos (con --reiniciar)..." : "Sembrando Expertos...");
  const filas = [];
  for (const e of EXPERTOS) {
    const fila = await sembrar(e);
    if (fila) filas.push(fila);
  }
  console.table(filas);
  console.log(`Listo: ${filas.length} Expertos en la colección "expertos".`);
}

main().catch((error) => {
  console.error("ERROR:", error.message);
  process.exitCode = 1;
});