package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time procedural voxel audio synthesizer for BLOCKMİNE.
 * Generates multi-layered sound effects (block break crunch, block place pop, jump whoosh,
 * template fanfare, reward chime, UI tap) and an ambient calm voxel piano melody loop.
 */
object SoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    var sfxVolume: Float = 0.85f

    @Volatile
    var musicVolume: Float = 0.55f

    @Volatile
    var isMusicEnabled: Boolean = false

    fun playBlockBreak() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            playSynthesizedBuffer(durationMs = 150) { t, progress ->
                val noise = (Random.nextFloat() * 2f - 1f) * 0.55f
                val tone = sin(2.0 * PI * (180.0 - 90.0 * progress) * t).toFloat() * 0.45f
                val env = exp(-6.0 * progress).toFloat()
                (noise + tone) * env * sfxVolume
            }
        }
    }

    fun playBlockPlace() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            playSynthesizedBuffer(durationMs = 110) { t, progress ->
                val freq = if (progress < 0.45) 310.0 else 440.0
                val tone = sin(2.0 * PI * freq * t).toFloat()
                val env = ((1.0 - progress) * (1.0 - progress)).toFloat()
                tone * env * 0.7f * sfxVolume
            }
        }
    }

    fun playJump() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            playSynthesizedBuffer(durationMs = 160) { t, progress ->
                val freq = 220.0 + 260.0 * progress
                val tone = sin(2.0 * PI * freq * t).toFloat()
                val env = (1.0 - progress).toFloat()
                tone * env * 0.55f * sfxVolume
            }
        }
    }

    fun playTemplateBuild() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            // Arpeggio C5 - E5 - G5 - C6
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            playSynthesizedBuffer(durationMs = 360) { t, progress ->
                val idx = (progress * notes.size).toInt().coerceIn(0, notes.lastIndex)
                val freq = notes[idx]
                val tone = (sin(2.0 * PI * freq * t) + 0.35 * sin(4.0 * PI * freq * t)).toFloat()
                val localP = (progress * notes.size) % 1.0
                val env = (1.0 - localP * 0.7).toFloat()
                tone * env * 0.5f * sfxVolume
            }
        }
    }

    fun playRewardClaim() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            val notes = doubleArrayOf(587.33, 740.0, 880.0, 1174.66)
            playSynthesizedBuffer(durationMs = 420) { t, progress ->
                val idx = (progress * notes.size).toInt().coerceIn(0, notes.lastIndex)
                val freq = notes[idx]
                val tone = (sin(2.0 * PI * freq * t) + 0.5 * sin(2.0 * PI * (freq * 1.5) * t)).toFloat()
                val env = (1.0 - progress * 0.5).toFloat()
                tone * env * 0.45f * sfxVolume
            }
        }
    }

    fun playUiClick() {
        if (sfxVolume <= 0.01f) return
        scope.launch {
            playSynthesizedBuffer(durationMs = 45) { t, progress ->
                val tone = sin(2.0 * PI * 680.0 * t).toFloat()
                tone * (1.0 - progress).toFloat() * 0.45f * sfxVolume
            }
        }
    }

    fun toggleBackgroundMusic(enable: Boolean) {
        isMusicEnabled = enable
        musicJob?.cancel()
        if (!enable) return
        musicJob = scope.launch {
            // Calm C418-style pentatonic ambient chord progression
            val melody = doubleArrayOf(
                261.63, 329.63, 392.00, 493.88,
                293.66, 369.99, 440.00, 523.25,
                220.00, 261.63, 329.63, 392.00
            )
            var step = 0
            while (isActive && isMusicEnabled) {
                if (musicVolume > 0.02f) {
                    val freq = melody[step % melody.size]
                    playSynthesizedBuffer(durationMs = 480) { t, progress ->
                        val warmPad = (sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 0.5) * t)).toFloat()
                        val attack = (progress * 5.0).coerceAtMost(1.0)
                        val decay = exp(-2.8 * progress)
                        (warmPad * attack * decay).toFloat() * 0.22f * musicVolume
                    }
                }
                step++
                delay(650L)
            }
        }
    }

    private fun playSynthesizedBuffer(
        durationMs: Int,
        sampleRate: Int = 22050,
        generator: (timeSec: Double, progress: Double) -> Float
    ) {
        try {
            val numSamples = (sampleRate * durationMs) / 1000
            if (numSamples <= 0) return
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val timeSec = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val sample = generator(timeSec, progress).coerceIn(-1f, 1f)
                pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Safe ignore in headless / Robolectric unit test environments
        }
    }
}
