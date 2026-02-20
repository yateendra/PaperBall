package com.example.paperball.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.*

class SoundManager {
    private val executor = Executors.newCachedThreadPool()
    private val sampleRate = 44100
    private val soundCache = mutableMapOf<SoundType, ShortArray>()
    
    enum class SoundType {
        SWISH,   // Scoring
        BOUNCE,  // Wall/Floor impact (Ping Pong Click)
        PERFECT, // Perfect shot chime
        FLICK    // Ball release (Ping Pong Paddle Hit)
    }

    init {
        // Pre-generate all sounds to avoid runtime calculation overhead
        soundCache[SoundType.SWISH] = generatePingPongCup()
        soundCache[SoundType.BOUNCE] = generatePingPongBounce()
        soundCache[SoundType.PERFECT] = generatePerfectChime()
        soundCache[SoundType.FLICK] = generatePingPongPaddle()
    }

    fun playSound(type: SoundType) {
        val audioData = soundCache[type] ?: return
        executor.execute {
            playRaw(audioData)
        }
    }

    private fun playRaw(data: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                
            audioTrack.write(data, 0, data.size)
            audioTrack.play()
            
            // Cleanup after play (approximate duration)
            Thread.sleep((data.size.toFloat() / sampleRate * 1000).toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generatePingPongBounce(): ShortArray {
        // High pitched "tock" sound characteristic of ping pong
        val durationSec = 0.04f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val frequency = 1400.0 // High resonance
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            // Very steep exponential decay for a crisp "click"
            val envelope = exp(-progress * 25.0)
            val sample = sin(2.0 * PI * frequency * i / sampleRate) * 0.8
            samples[i] = (sample * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun generatePingPongPaddle(): ShortArray {
        // Lower, woodier "knock" for the initial flick
        val durationSec = 0.05f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val frequency = 700.0
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val envelope = exp(-progress * 15.0)
            // Add a bit of noise for texture
            val noise = (Math.random() * 2.0 - 1.0) * 0.1
            val sample = (sin(2.0 * PI * frequency * i / sampleRate) + noise) * 0.7
            samples[i] = (sample * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun generatePingPongCup(): ShortArray {
        // Hollow "plop" for entering the cup
        val durationSec = 0.12f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            // Sliding frequency for a "ploop" effect
            val freq = 600.0 * (1.0 - progress * 0.4)
            val envelope = sin(PI * progress) * exp(-progress * 4.0)
            val sample = sin(2.0 * PI * freq * i / sampleRate) * 0.6
            samples[i] = (sample * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun generatePerfectChime(): ShortArray {
        // Premium crystalline chime for perfect shots
        val durationSec = 0.5f
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        
        val freq1 = 1760.0 // High A
        val freq2 = 2637.0 // High E
        
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val envelope = exp(-progress * 6.0)
            val sample = (sin(2.0 * PI * freq1 * i / sampleRate) + 
                         0.5 * sin(2.0 * PI * freq2 * i / sampleRate)) * 0.4
            samples[i] = (sample * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }
}
