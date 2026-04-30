package com.example.stemselector.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class StemPlayer(coroutineScope: CoroutineScope) {
    // Single output device that sends mixed audio to the speakers. There's only one because multiple instances would drift out of sync
    var audioTrack: AudioTrack? = null

    var bufferSize: Int = 0
    var stemPaths: List<String> = emptyList()

    // Each stem gets own stream to be read incrementally without loading full files into memory
    var stemStreams: MutableMap<String, InputStream> = mutableMapOf()

    // Mixing loop checks this for each stem on every buffer cycle, zeroes out stem's buffer before mixing to mute it
    var mutedStems: MutableMap<String, Boolean> = mutableMapOf()

    // Used by the scrubber UI to show position, and by seekTo() to know where to skip to in each stream
    var currentPositionBytes: Long = 0

    var totalSongLengthInBytes: Long = 0

    // The mixing loop checks this on every cycle. When pause() or stop() sets it to false, the loop exits cleanly.
    var isPlaying = false

    // Mixing loop runs continuously on a background thread so it doesn't freeze the UI
    val coroutineScope = coroutineScope

    fun play(stemPaths: List<String>) {
        this.stemPaths = stemPaths
        setupAudioTrack()
        startMixingLoop()
    }

    fun pause() {
        isPlaying = false
        audioTrack?.pause()
    }

    fun stop() {
        isPlaying = false
        audioTrack?.stop()
        currentPositionBytes = 0
        for (stream in stemStreams) {
            stream.value.close()
        }
        stemStreams.clear()
    }

    fun setMuted(stemPath: String, isMuted: Boolean) {
        mutedStems[stemPath] = isMuted
    }

    suspend fun seekTo(positionBytes: Long) {
        val wasPlaying = isPlaying

        isPlaying = false
        delay(100)

        audioTrack?.pause()
        audioTrack?.flush()

        withContext(Dispatchers.IO) {
            for ((stemPath, stream) in stemStreams) {
                stream.close()
                stemStreams[stemPath] = FileInputStream(stemPath)
                stemStreams[stemPath]?.skip(positionBytes)
            }
        }

        currentPositionBytes = positionBytes

        if (wasPlaying) {
            startMixingLoop()
        }
    }

    private fun setupAudioTrack() {
        if (audioTrack == null) {
            // Calculate minimum safe buffer size
            bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)

            // Build new audiotrack with buffer
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(44100)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Open a stream for each stem file
            for (stem: String in stemPaths) {
                stemStreams[stem] = FileInputStream(stem)
            }

            // Calculate song length
            totalSongLengthInBytes = File(stemStreams.keys.first()).length()
        }
    }

    private fun startMixingLoop() {
        audioTrack?.play()
        isPlaying = true

        // Launch mixing loop coroutine
        coroutineScope.launch(Dispatchers.IO) {
            val mixBuffer = ShortArray(bufferSize / 2) // Divide by 2 because each short = 2 bytes
            val stemBuffer = ByteArray(bufferSize)

            while (isPlaying) {
                // Clear mix buffer each cycle so previous audio doesn't bleed through
                mixBuffer.fill(0)

                for ((stemPath, stream) in stemStreams) {
                    val bytesRead = stream.read(stemBuffer)

                    // If any stem runs out of data, playback is complete
                    if (bytesRead <= 0) {
                        isPlaying = false
                        break
                    }

                    // Skip mixing if this stem is muted
                    if (mutedStems[stemPath] == true) continue

                    // Convert bytes to 16-bit samples and mix
                    var i = 0
                    while (i < bytesRead - 1) {
                        // Combine two bytes into on Short (little-endian PCM)
                        val sample =
                            (stemBuffer[i].toInt() and 0xff) or (stemBuffer[i + 1].toInt() shl 8)
                        val currentMix = mixBuffer[i / 2].toInt()

                        // Add sample to mix, clamp to Short range to prevent distortion
                        mixBuffer[i / 2] = (currentMix + sample).coerceIn(
                            Short.MIN_VALUE.toInt(),
                            Short.MAX_VALUE.toInt()
                        ).toShort()
                        i += 2
                    }
                }
                audioTrack!!.write(mixBuffer, 0, mixBuffer.size, AudioTrack.WRITE_BLOCKING)
                currentPositionBytes += bufferSize
            }

            audioTrack!!.stop()
        }
    }
}