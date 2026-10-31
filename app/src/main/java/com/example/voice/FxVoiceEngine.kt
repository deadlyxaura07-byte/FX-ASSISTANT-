package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.VibeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class FxVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentSpokenText = MutableStateFlow("")
    val currentSpokenText: StateFlow<String> = _currentSpokenText.asStateFlow()

    private val _vibeMode = MutableStateFlow(VibeMode.GEN_Z)
    val vibeMode: StateFlow<VibeMode> = _vibeMode.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null

    init {
        initTts()
    }

    fun setVibe(vibe: VibeMode) {
        _vibeMode.value = vibe
        // Adjust pitch and rate for flavor
        when (vibe) {
            VibeMode.GEN_Z -> {
                tts?.setPitch(1.15f)
                tts?.setSpeechRate(1.1f)
            }
            VibeMode.BRAINROT_ALPHA -> {
                tts?.setPitch(1.25f)
                tts?.setSpeechRate(1.2f)
            }
            VibeMode.BALANCED_COOL -> {
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.05f)
            }
            VibeMode.PRO_EXECUTIVE -> {
                tts?.setPitch(0.95f)
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language not supported for TTS")
            } else {
                isTtsInitialized = true
                tts?.setPitch(1.1f)
                tts?.setSpeechRate(1.08f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun speak(rawText: String, overrideVibe: VibeMode? = null) {
        val targetVibe = overrideVibe ?: _vibeMode.value
        val formattedSpeech = formatTextForVibe(rawText, targetVibe)
        _currentSpokenText.value = formattedSpeech

        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS not ready, simulated speech: $formattedSpeech")
            _isSpeaking.value = true
            // Auto reset after 2 seconds if TTS offline
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _isSpeaking.value = false
            }, 2500L)
            return
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "fx_speech_${System.currentTimeMillis()}")
        tts?.speak(formattedSpeech, TextToSpeech.QUEUE_FLUSH, params, "fx_speech_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    Log.w(TAG, "SpeechRecognizer error: $error")
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        onSpeechRecognized?.invoke(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _currentSpokenText.value = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    companion object {
        private const val TAG = "FxVoiceEngine"

        fun formatTextForVibe(text: String, vibe: VibeMode): String {
            return when (vibe) {
                VibeMode.GEN_Z -> {
                    // Inject natural Gen Z flow if not already present
                    if (!text.contains("cap", ignoreCase = true) && !text.contains("bet", ignoreCase = true)) {
                        val intros = listOf("Bet, ", "No cap, ", "Alright fam, ", "Real talk: ")
                        val outros = listOf(" We're so back.", " It's giving main character.", " Period.", " No joke.")
                        val intro = intros.random()
                        val outro = outros.random()
                        "$intro$text$outro"
                    } else text
                }
                VibeMode.BRAINROT_ALPHA -> {
                    if (!text.contains("rizz", ignoreCase = true) && !text.contains("skibidi", ignoreCase = true)) {
                        val intros = listOf("Yo gigachad! ", "Skibidi alert! ", "Sigma grindset check: ", "W energy! ")
                        val outros = listOf(" Pure level 10 rizz.", " Gigachad approved.", " Keep cookin boss.")
                        val intro = intros.random()
                        val outro = outros.random()
                        "$intro$text$outro"
                    } else text
                }
                VibeMode.BALANCED_COOL -> {
                    "Gotchu. $text"
                }
                VibeMode.PRO_EXECUTIVE -> {
                    text
                }
            }
        }
    }
}
