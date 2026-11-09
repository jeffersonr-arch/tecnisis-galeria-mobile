package com.tecnisis.galeria.domain.model

import kotlin.math.roundToInt

/** HU08: ML Kit solo muestra las etiquetas con 50 % de confianza o más. */
const val CONFIANZA_MINIMA = 0.5f

/** HU08: como máximo 5 etiquetas por obra, las de mayor confianza. */
const val MAXIMO_ETIQUETAS = 5

/** Lo que devuelve ML Kit Image Labeling: el texto en inglés y la confianza (0 a 1). */
data class EtiquetaCruda(val texto: String, val confianza: Float)

/** Lo que se muestra: el nombre (en español si está en el diccionario), el porcentaje (0 a 100) y si se tradujo. */
data class EtiquetaDetectada(val nombre: String, val porcentaje: Int, val traducida: Boolean)

/**
 * Aplica las reglas de la HU08: confianza ≥ 50 %, de mayor a menor, máximo 5 y traducidas con el diccionario.
 * La que no está en el diccionario se muestra en inglés (traducida = false).
 */
fun etiquetasVisibles(crudas: List<EtiquetaCruda>): List<EtiquetaDetectada> =
    crudas
        .filter { it.confianza >= CONFIANZA_MINIMA }
        .sortedByDescending { it.confianza }
        .take(MAXIMO_ETIQUETAS)
        .map { etiqueta ->
            val espanol = DICCIONARIO_ETIQUETAS[etiqueta.texto]
            EtiquetaDetectada(
                nombre = espanol ?: etiqueta.texto,
                porcentaje = (etiqueta.confianza * 100).roundToInt().coerceIn(0, 100),
                traducida = espanol != null
            )
        }

/**
 * Inglés → español de las etiquetas del modelo base de ML Kit que más pueden salir en fotos de obras.
 * Todas están escritas tal cual en la lista oficial del modelo (developers.google.com/ml-kit/vision/image-labeling/label-map).
 * El modelo base no tiene "Painting" ni "Art": para un cuadro suele devolver lo que se ve en él (Sky, Boat, Flower…).
 */
val DICCIONARIO_ETIQUETAS: Map<String, String> = mapOf(
    // Materiales, técnicas y objetos de arte
    "Watercolor paint" to "Acuarela",
    "Pattern" to "Patrón",
    "Statue" to "Estatua",
    "Monument" to "Monumento",
    "Porcelain" to "Porcelana",
    "Textile" to "Textil",
    "Wool" to "Lana",
    "Cotton" to "Algodón",
    "Crochet" to "Crochet",
    "Knitting" to "Tejido de punto",
    "Needlework" to "Bordado",
    "Quilting" to "Acolchado",
    "Paper" to "Papel",
    "Poster" to "Póster",
    "Comics" to "Cómic",
    "Monochrome" to "Monocromo",
    "Miniature" to "Miniatura",
    "Flowerpot" to "Maceta",
    "Tableware" to "Vajilla",
    "Leather" to "Cuero",
    "Metal" to "Metal",
    "Brick" to "Ladrillo",
    "Tile" to "Azulejo",
    "Wall" to "Pared",
    "Jewellery" to "Joyería",
    "Necklace" to "Collar",
    "Musical instrument" to "Instrumento musical",
    "Museum" to "Museo",
    // Paisaje y naturaleza
    "Sky" to "Cielo",
    "Sunset" to "Atardecer",
    "Cliff" to "Acantilado",
    "Beach" to "Playa",
    "Sand" to "Arena",
    "Lake" to "Lago",
    "River" to "Río",
    "Waterfall" to "Catarata",
    "Mountain" to "Montaña",
    "Volcano" to "Volcán",
    "Desert" to "Desierto",
    "Field" to "Campo",
    "Forest" to "Bosque",
    "Garden" to "Jardín",
    "Flower" to "Flor",
    "Petal" to "Pétalo",
    "Plant" to "Planta",
    "Flora" to "Flora",
    "Branch" to "Rama",
    "Rock" to "Roca",
    "Fog" to "Niebla",
    "Rainbow" to "Arcoíris",
    "Moon" to "Luna",
    "Star" to "Estrella",
    "Fire" to "Fuego",
    "Underwater" to "Bajo el agua",
    "Shell" to "Concha",
    // Seres vivos y personas
    "Bird" to "Ave",
    "Horse" to "Caballo",
    "Dog" to "Perro",
    "Cat" to "Gato",
    "Cattle" to "Ganado",
    "Fruit" to "Fruta",
    "Food" to "Comida",
    "Person" to "Persona",
    "Smile" to "Sonrisa",
    "Hand" to "Mano",
    "Dress" to "Vestido",
    "Hat" to "Sombrero",
    // Construcciones y lugares
    "Boat" to "Barco",
    "Sailboat" to "Velero",
    "Pier" to "Muelle",
    "Bridge" to "Puente",
    "Road" to "Camino",
    "Building" to "Edificio",
    "Skyline" to "Horizonte urbano",
    "Church" to "Iglesia",
    "Temple" to "Templo",
    "Castle" to "Castillo",
    "Palace" to "Palacio",
    "Ruins" to "Ruinas",
    // Otros
    "Heart" to "Corazón"
)