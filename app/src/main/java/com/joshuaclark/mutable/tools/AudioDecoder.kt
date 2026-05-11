package com.joshuaclark.mutable.tools

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun decodeAudioFile(context: Context, uri: Uri) : ByteArray? {
    val mediaExtractor = MediaExtractor()
    mediaExtractor.setDataSource(context, uri, null)

    var audioFormat: MediaFormat? = null
    var audioMIME: String? = null

    for (i in 0 until mediaExtractor.trackCount) {
        val format: MediaFormat = mediaExtractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime != null && mime.startsWith("audio/")) {
            mediaExtractor.selectTrack(i)
            audioFormat = format
            audioMIME = mime
        }
    }

    if (audioMIME != null) {
        val codec = MediaCodec.createDecoderByType(audioMIME)
        codec.configure(audioFormat, null, null, 0)
        codec.start()

        val decodedAudio = ByteArrayOutputStream()
        var inputDone = false

        val bufferInfo = MediaCodec.BufferInfo()
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    }
                    else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    }
                }
            }
            if (!outputDone) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer: ByteBuffer = codec.getOutputBuffer(outputIndex)!!
                    val byteArray = ByteArray(bufferInfo.size)
                    outputBuffer.get(byteArray)
                    decodedAudio.write(byteArray)
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        }
        // Cleanup
        codec.stop()
        codec.release()
        mediaExtractor.release()
        return decodedAudio.toByteArray()
    }
    else
    {
        return null
    }
}