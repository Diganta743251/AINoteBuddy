package com.ainotebuddy.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive Text-to-Speech helper with advanced features
 */

@Singleton
class TextToSpeechHelper @Inject constructor(
    private val context: Context
) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _initializationState = MutableStateFlow(TTSInitializationState.INITIALIZING)
    val initializationState: StateFlow<TTSInitializationState> = _initializationState.asStateFlow()
    
    private val _speakingState = MutableStateFlow(false)
    val speakingState: StateFlow<Boolean> = _speakingState.asStateFlow()
    
    private val _availableLanguages = MutableStateFlow<List<TTSLanguage>>(emptyList())
    val availableLanguages: StateFlow<List<TTSLanguage>> = _availableLanguages.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow<TTSLanguage?>(null)
    val currentLanguage: StateFlow<TTSLanguage?> = _currentLanguage.asStateFlow()
    
    private val utteranceCallbacks = mutableMapOf<String, TTSCallback>()
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        scope.launch {
            _initializationState.value = TTSInitializationState.INITIALIZING
            
            try {
                tts = TextToSpeech(context) { status ->
                    scope.launch {
                        if (status == TextToSpeech.SUCCESS) {
                            isInitialized = true
                            setupTTS()
                            loadAvailableLanguages()
                            _initializationState.value = TTSInitializationState.READY
                        } else {
                            _initializationState.value = TTSInitializationState.ERROR
                        }
                    }
                }
            } catch (e: Exception) {
                _initializationState.value = TTSInitializationState.ERROR
            }
        }
    }
    
    private fun setupTTS() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _speakingState.value = true
                    utteranceId?.let { id ->
                        utteranceCallbacks[id]?.onStart?.invoke()
                    }
                }
            }
            
            override fun onDone(utteranceId: String?) {
                scope.launch {
                    _speakingState.value = false
                    utteranceId?.let { id ->
                        utteranceCallbacks[id]?.onComplete?.invoke()
                        utteranceCallbacks.remove(id)
                    }
                }
            }
            
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _speakingState.value = false
                    utteranceId?.let { id ->
                        utteranceCallbacks[id]?.onError?.invoke("TTS Error")
                        utteranceCallbacks.remove(id)
                    }
                }
            }
            
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                utteranceId?.let { id ->
                    utteranceCallbacks[id]?.onProgress?.invoke(start, end)
                }
            }
        })
    }
    
    private fun loadAvailableLanguages() {
        scope.launch(Dispatchers.IO) {
            val languages = mutableListOf<TTSLanguage>()
            
            // Common languages to check
            val languagesToCheck = listOf(
                Locale.ENGLISH to "English",
                Locale("es", "") to "Spanish",
                Locale.FRENCH to "French",
                Locale.GERMAN to "German",
                Locale.ITALIAN to "Italian",
                Locale("pt", "") to "Portuguese",
                Locale("ru", "") to "Russian",
                Locale.CHINESE to "Chinese",
                Locale.JAPANESE to "Japanese",
                Locale("ko", "") to "Korean",
                Locale("hi", "") to "Hindi",
                Locale("ar", "") to "Arabic"
            )
            
            languagesToCheck.forEach { (locale, displayName) ->
                val result = tts?.isLanguageAvailable(locale)
                if (result == TextToSpeech.LANG_AVAILABLE || 
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    
                    languages.add(
                        TTSLanguage(
                            locale = locale,
                            displayName = displayName,
                            isAvailable = true
                        )
                    )
                }
            }
            
            _availableLanguages.value = languages
            
            // Set default language
            if (languages.isNotEmpty()) {
                val defaultLanguage = languages.find { it.locale == Locale.getDefault() }
                    ?: languages.first()
                setLanguage(defaultLanguage.locale)
            }
        }
    }
    
    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        callback: TTSCallback? = null
    ): String? {
        if (!isInitialized || tts == null) return null
        
        val utteranceId = UUID.randomUUID().toString()
        
        callback?.let { utteranceCallbacks[utteranceId] = it }
        
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        tts?.apply {
            setPitch(pitch)
            setSpeechRate(speechRate)
            speak(text, queueMode, params, utteranceId)
        }
        
        return utteranceId
    }
    
    fun speakWithSSML(
        ssmlText: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        callback: TTSCallback? = null
    ): String? {
        if (!isInitialized || tts == null) return null
        
        val utteranceId = UUID.randomUUID().toString()
        callback?.let { utteranceCallbacks[utteranceId] = it }
        
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        // For SSML support, we would need to check if the engine supports it
        // For now, we'll strip SSML tags and speak as plain text
        val plainText = ssmlText.replace(Regex("<[^>]*>"), "")
        
        tts?.speak(plainText, queueMode, params, utteranceId)
        
        return utteranceId
    }
    
    fun pause() {
        tts?.stop()
        _speakingState.value = false
    }
    
    fun stop() {
        tts?.stop()
        _speakingState.value = false
        utteranceCallbacks.clear()
    }
    
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        val success = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        
        if (success) {
            _currentLanguage.value = _availableLanguages.value.find { it.locale == locale }
        }
        
        return success
    }
    
    fun setVoice(voiceName: String): Boolean {
        return try {
            val voices = tts?.voices
            val voice = voices?.find { it.name == voiceName }
            voice?.let {
                tts?.voice = it
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAvailableVoices(): List<TTSVoice> {
        return try {
            tts?.voices?.map { voice ->
                TTSVoice(
                    name = voice.name,
                    locale = voice.locale,
                    quality = when (voice.quality) {
                        android.speech.tts.Voice.QUALITY_VERY_HIGH -> TTSVoiceQuality.VERY_HIGH
                        android.speech.tts.Voice.QUALITY_HIGH -> TTSVoiceQuality.HIGH
                        android.speech.tts.Voice.QUALITY_NORMAL -> TTSVoiceQuality.NORMAL
                        else -> TTSVoiceQuality.LOW
                    },
                    features = voice.features ?: emptySet()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun speakLongText(
        text: String,
        maxChunkSize: Int = 4000,
        pauseBetweenChunks: Long = 500,
        callback: TTSCallback? = null
    ) {
        scope.launch {
            val chunks = text.chunked(maxChunkSize)
            
            chunks.forEachIndexed { index, chunk ->
                val isLast = index == chunks.size - 1
                
                val chunkCallback = if (isLast) callback else TTSCallback(
                    onComplete = {
                        scope.launch {
                            delay(pauseBetweenChunks)
                        }
                    }
                )
                
                speak(chunk, TextToSpeech.QUEUE_ADD, callback = chunkCallback)
                
                if (!isLast) {
                    delay(pauseBetweenChunks)
                }
            }
        }
    }
    
    fun isSpeaking(): Boolean = _speakingState.value
    
    fun cleanup() {
        scope.cancel()
        utteranceCallbacks.clear()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

// Composable function for easy TTS integration
@Composable
fun rememberTextToSpeechHelper(): TextToSpeechHelper {
    val context = LocalContext.current
    val helper = remember { TextToSpeechHelper(context) }
    
    DisposableEffect(helper) {
        onDispose { helper.cleanup() }
    }
    
    return helper
}

// Data classes
data class TTSLanguage(
    val locale: Locale,
    val displayName: String,
    val isAvailable: Boolean
)

data class TTSVoice(
    val name: String,
    val locale: Locale,
    val quality: TTSVoiceQuality,
    val features: Set<String>
)

enum class TTSVoiceQuality {
    VERY_HIGH, HIGH, NORMAL, LOW
}

enum class TTSInitializationState {
    INITIALIZING, READY, ERROR
}

data class TTSCallback(
    val onStart: (() -> Unit)? = null,
    val onComplete: (() -> Unit)? = null,
    val onError: ((String) -> Unit)? = null,
    val onProgress: ((start: Int, end: Int) -> Unit)? = null
)

// TTS Settings data class
data class TTSSettings(
    val language: Locale = Locale.getDefault(),
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voice: String? = null,
    val enabled: Boolean = true,
    val readPunctuation: Boolean = true,
    val readNumbersAsDigits: Boolean = false
)

// TTS Configuration builder
class TTSConfigurationBuilder {
    private var pitch: Float = 1.0f
    private var speechRate: Float = 1.0f
    private var language: Locale = Locale.getDefault()
    private var voice: String? = null
    
    fun pitch(pitch: Float) = apply { this.pitch = pitch }
    fun speechRate(rate: Float) = apply { this.speechRate = rate }
    fun language(locale: Locale) = apply { this.language = locale }
    fun voice(voiceName: String) = apply { this.voice = voiceName }
    
    fun build() = TTSSettings(
        pitch = pitch,
        speechRate = speechRate,
        language = language,
        voice = voice
    )
}

// Utility functions
object TTSUtils {
    fun createSSML(
        text: String,
        pauseMs: Long? = null,
        emphasis: String? = null,
        prosodyRate: String? = null,
        prosodyPitch: String? = null
    ): String {
        val builder = StringBuilder()
        builder.append("<speak>")
        
        if (pauseMs != null) {
            builder.append("<break time=\"${pauseMs}ms\"/>")
        }
        
        if (emphasis != null) {
            builder.append("<emphasis level=\"$emphasis\">")
        }
        
        if (prosodyRate != null || prosodyPitch != null) {
            builder.append("<prosody")
            prosodyRate?.let { builder.append(" rate=\"$it\"") }
            prosodyPitch?.let { builder.append(" pitch=\"$it\"") }
            builder.append(">")
        }
        
        builder.append(text)
        
        if (prosodyRate != null || prosodyPitch != null) {
            builder.append("</prosody>")
        }
        
        if (emphasis != null) {
            builder.append("</emphasis>")
        }
        
        builder.append("</speak>")
        return builder.toString()
    }
    
    fun estimateSpeechDuration(
        text: String,
        wordsPerMinute: Int = 150
    ): Long {
        val wordCount = text.split("\\s+".toRegex()).size
        return (wordCount * 60 * 1000L) / wordsPerMinute
    }
    
    fun preprocessTextForSpeech(text: String): String {
        return text
            .replace(Regex("\\b(URL|HTTP|HTTPS)\\b", RegexOption.IGNORE_CASE), "link")
            .replace(Regex("\\b\\w+@\\w+\\.\\w+\\b"), "email address")
            .replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "phone number")
            .replace(Regex("\\$\\d+\\.?\\d*"), "dollar amount")
            .replace(Regex("\\d+%"), "percent")
    }
}