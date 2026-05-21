package com.joshuaclark.mutable.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.collections.iterator

class StemPlayer(
    val coroutineScope: CoroutineScope //Used to run mixing loop continuously on a background thread so it doesn't freeze the UI
) {
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
    var isPlaying: Boolean = false

    var isLooping: Boolean = false

    // Prevents race condition error where releasing the audioTrack causes the mix loop to crash
    private var playbackJob: Job? = null

    fun play(stemPaths: List<String>) {
        if (isPlaying) { return} // Guard against pressing Play twice crash
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
        audioTrack?.pause()
        audioTrack?.flush()

        // Reset streams to beginning
        for ((stemPath, stream) in stemStreams) {
            stream.close()
            stemStreams[stemPath] = FileInputStream(stemPath)
        }

        currentPositionBytes = 0
    }

    fun loop(isLooping: Boolean) {
        this.isLooping = isLooping
    }

    fun setMuted(stemPath: String, isMuted: Boolean) {
        mutedStems[stemPath] = isMuted
    }

    suspend fun seekTo(positionBytes: Long) {
        val wasPlaying = isPlaying
        val alignedPosition = positionBytes - (positionBytes % 4)

        isPlaying = false
        delay(100)

        audioTrack?.pause()
        audioTrack?.flush()

        withContext(Dispatchers.IO) {
            for ((stemPath, stream) in stemStreams) {
                stream.close()
                stemStreams[stemPath] = FileInputStream(stemPath)
                stemStreams[stemPath]?.skip(alignedPosition)
            }
        }

        currentPositionBytes = alignedPosition

        if (wasPlaying) {
            startMixingLoop()
        }
    }

    // Free up all resources ready for new song to be selected
    fun teardown() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        currentPositionBytes = 0
        for (stream in stemStreams) {
            stream.value.close()
        }
        stemStreams.clear()
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
        val track = audioTrack ?: return
        if (track.state != AudioTrack.STATE_INITIALIZED) return

        isPlaying = true
        track.play()

        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val mixBuffer = ShortArray(bufferSize / 2)
                val stemBuffer = ByteArray(bufferSize)

                while (isPlaying && isActive) {
                    mixBuffer.fill(0)

                    for ((stemPath, stream) in stemStreams) {
                        var bytesRead = stream.read(stemBuffer)

                        if (bytesRead < bufferSize) {
                            if (!isLooping) {
                                stop()
                                break
                            }
                            stemStreams[stemPath]?.close()
                            stemStreams[stemPath] = FileInputStream(stemPath)
                            currentPositionBytes = 0

                            if (bytesRead < 0) bytesRead = 0
                            val remaining = bufferSize - bytesRead
                            stemStreams[stemPath]?.read(stemBuffer, bytesRead, remaining)
                            bytesRead = bufferSize
                        }

                        if (mutedStems[stemPath] == true) continue

                        var i = 0
                        while (i < bytesRead - 1) {
                            val sample = (stemBuffer[i].toInt() and 0xff) or
                                    (stemBuffer[i + 1].toInt() shl 8)
                            val currentMix = mixBuffer[i / 2].toInt()
                            mixBuffer[i / 2] = (currentMix + sample).coerceIn(
                                Short.MIN_VALUE.toInt(),
                                Short.MAX_VALUE.toInt()
                            ).toShort()
                            i += 2
                        }
                    }

                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.write(mixBuffer, 0, mixBuffer.size, AudioTrack.WRITE_BLOCKING)
                        currentPositionBytes += bufferSize
                    }
                }
            } catch (e: Exception) {
                Log.e("StemPlayer", "Error in mixing loop: ${e.message}")
            } finally {
                stopTrackSafely(track)
            }
        }
    }

    private fun stopTrackSafely(track: AudioTrack) {
        try {
            if (track.state == AudioTrack.STATE_INITIALIZED &&
                track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop()
            }
        } catch (e: IllegalStateException) {
            // This happens if teardown() calls track.release() while this coroutine is mid-execution
            Log.d("StemPlayer", "Track already released, skipping stop()")
        }
    }
}