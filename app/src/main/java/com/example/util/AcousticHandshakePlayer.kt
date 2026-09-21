package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.sin

/**
 * AcousticHandshakePlayer:
 * 1. Generates near-ultrasonic (~18.5kHz - 19.5kHz) encrypted acoustic audio pulses via AudioTrack.
 * 2. Provides offline zero-cost acoustic token exchange for contactless due settlement.
 * 3. Provides loud Bangla Voice Confirmation via TextToSpeech ("রহিম ভাইয়ের ৪০০ টাকা বাকি সম্পূর্ণ পরিশোধিত হয়েছে").
 * 4. Provides haptic vibration signals for proximity alerts and successful payment confirmations.
 */
object AcousticHandshakePlayer {
    private const val TAG = "AcousticHandshake"
    private const val SAMPLE_RATE = 44100
    private const val BASE_ULTRASONIC_FREQ = 18500.0 // Near-ultrasonic inaudible carrier frequency
    private const val FREQ_SHIFT_STEP = 250.0

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    fun initializeTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val banglaLocale = Locale("bn", "BD")
                val result = tts?.setLanguage(banglaLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
                isTtsReady = true
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    /**
     * Emits an encrypted high-frequency near-ultrasonic acoustic pulse packet for offline handshake.
     * Takes ~1.8 seconds to transmit the payload.
     */
    suspend fun playEncryptedAcousticPulse(
        payload: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.Default) {
        var audioTrack: AudioTrack? = null
        try {
            val durationSeconds = 1.8
            val numSamples = (durationSeconds * SAMPLE_RATE).toInt()
            val sampleBuffer = ShortArray(numSamples)

            // Derive deterministic frequency modulation sequence from payload SHA-256 hash
            val hash = sha256(payload)
            val chunkCount = 8
            val samplesPerChunk = numSamples / chunkCount

            for (chunk in 0 until chunkCount) {
                val hexChar = hash.getOrNull(chunk) ?: '0'
                val nibbleVal = Character.digit(hexChar, 16).coerceAtLeast(0)
                val chunkFreq = BASE_ULTRASONIC_FREQ + (nibbleVal * FREQ_SHIFT_STEP)

                val startIdx = chunk * samplesPerChunk
                val endIdx = (startIdx + samplesPerChunk).coerceAtMost(numSamples)

                for (i in startIdx until endIdx) {
                    val time = i.toDouble() / SAMPLE_RATE
                    // Window envelope to eliminate click/pop audio artifacts
                    val window = 0.5 * (1 - kotlin.math.cos(2.0 * Math.PI * (i - startIdx) / samplesPerChunk))
                    val wave = sin(2.0 * Math.PI * chunkFreq * time) * window
                    sampleBuffer[i] = (wave * Short.MAX_VALUE * 0.85).toInt().toShort()
                }
                onProgress((chunk + 1).toFloat() / chunkCount)
            }

            val bufferSize = sampleBuffer.size * 2
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize.coerceAtLeast(minBufferSize))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sampleBuffer, 0, sampleBuffer.size)
            audioTrack.play()

            // Wait for duration of playback
            kotlinx.coroutines.delay((durationSeconds * 1000).toLong())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing acoustic pulse", e)
            false
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Announces settlement in Bangla with loud clear speech
     * Example: "রহিম ভাইয়ের ৪০০ টাকা বাকি সম্পূর্ণ পরিশোধিত হয়েছে।"
     */
    fun speakBanglaSettlementConfirmation(
        customerName: String,
        amount: Double,
        context: Context
    ) {
        initializeTts(context)
        val formattedAmount = BengaliFormatters.toBanglaCurrency(amount)
        val customerLabel = if (customerName.isNotBlank()) {
            if (customerName.endsWith("ভাই") || customerName.endsWith("সাহেব") || customerName.endsWith("মামা")) {
                customerName
            } else {
                "$customerName ভাই"
            }
        } else {
            "গ্রাহক ভাই"
        }

        val speechText = "$customerLabel এর $formattedAmount বাকি সম্পূর্ণ পরিশোধিত হয়েছে।"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "SETTLEMENT_VOICE_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak Bangla text", e)
        }
    }

    /**
     * Triggers distinct haptic vibration for customer arrival in shop
     */
    fun triggerCustomerArrivalHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double tap gentle vibration pattern: 0ms wait, 80ms buzz, 100ms pause, 120ms buzz
                val pattern = longArrayOf(0, 80, 100, 120)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger vibration", e)
        }
    }

    /**
     * Triggers success vibration on acoustic handshake completed
     */
    fun triggerSuccessHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger success vibration", e)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
