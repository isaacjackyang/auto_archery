package com.autoarchery.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class SpeechCoordinator(
    context: Context,
) : TextToSpeech.OnInitListener {
    private val applicationContext = context.applicationContext
    private val pendingQueue = ConcurrentLinkedQueue<Pair<String, Locale>>()
    private val textToSpeech: TextToSpeech = TextToSpeech(applicationContext, this)

    @Volatile
    private var isReady: Boolean = false

    @Volatile
    private var currentLocale: Locale = Locale.TRADITIONAL_CHINESE

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (!isReady) return

        while (true) {
            val item = pendingQueue.poll() ?: break
            speakInternal(item.first, item.second)
        }
    }

    fun speak(text: String, languageCode: String) {
        val locale = resolveLocale(languageCode)
        if (!isReady) {
            pendingQueue.add(text to locale)
            return
        }
        speakInternal(text, locale)
    }

    fun stop() {
        if (isReady) {
            textToSpeech.stop()
        } else {
            pendingQueue.clear()
        }
    }

    fun shutdown() {
        pendingQueue.clear()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun speakInternal(text: String, locale: Locale) {
        if (currentLocale != locale) {
            textToSpeech.language = locale
            currentLocale = locale
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    private fun resolveLocale(languageCode: String): Locale =
        when (languageCode.lowercase()) {
            "zh-tw", "zh_tw", "zh-hant", "zh-hant-tw" -> Locale.TRADITIONAL_CHINESE
            else -> Locale.ENGLISH
        }
}
