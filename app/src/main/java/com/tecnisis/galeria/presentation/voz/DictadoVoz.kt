package com.tecnisis.galeria.presentation.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Lo que va pasando mientras el socio dicta. */
sealed interface EventoDictado {
    data object Escuchando : EventoDictado
    data class Parcial(val texto: String) : EventoDictado   // lo que va entendiendo
    data class Final(val texto: String) : EventoDictado     // terminó la frase
    data class Fallo(val sinVoz: Boolean) : EventoDictado   // sinVoz = no escuchó nada (no es grave)
}

/** HU06: dictado con el reconocedor de voz de Android. No graba audio: solo devuelve texto. */
class DictadoVoz(private val contexto: Context) {

    fun estaDisponible(): Boolean = SpeechRecognizer.isRecognitionAvailable(contexto)

    /**
     * Escucha una frase y se detiene sola tras un silencio. Se recolecta en el hilo principal
     * (rememberCoroutineScope). Si se cancela la recolección, se apaga el micrófono.
     */
    fun escuchar(): Flow<EventoDictado> = callbackFlow {
        val reconocedor = SpeechRecognizer.createSpeechRecognizer(contexto)
        reconocedor.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(EventoDictado.Escuchando)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                primerTexto(partialResults)?.let { trySend(EventoDictado.Parcial(it)) }
            }

            override fun onResults(results: Bundle?) {
                trySend(EventoDictado.Final(primerTexto(results).orEmpty()))
                close()
            }

            override fun onError(error: Int) {
                val sinVoz = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                trySend(EventoDictado.Fallo(sinVoz))
                close()
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        reconocedor.startListening(intentDeDictado())
        awaitClose { reconocedor.destroy() }
    }

    private fun intentDeDictado() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun primerTexto(datos: Bundle?): String? =
        datos?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
}