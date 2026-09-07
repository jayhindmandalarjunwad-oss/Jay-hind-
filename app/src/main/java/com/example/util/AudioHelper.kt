package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

object AudioRecorderHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording by mutableStateOf(false)
        private set
    var recordingDurationSeconds by mutableIntStateOf(0)
        private set

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun startRecording(context: Context): Boolean {
        return try {
            stopRecording() // ensure any previous is stopped

            val outputDir = File(context.cacheDir, "voice_notes").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "JayHind_Voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1) // Mono for voice - reduces size by 50%
                // Ultra-low data bitrate (24 kbps = ~180 KB/min sweet spot between 120-200 KB)
                setAudioEncodingBitRate(24000)
                setAudioSamplingRate(22050) // Crisp speech clarity with minimal bandwidth
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            recordingDurationSeconds = 0

            timerJob?.cancel()
            timerJob = scope.launch {
                while (isRecording) {
                    delay(500)
                    val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                    recordingDurationSeconds = elapsed.toInt()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            cleanup()
            false
        }
    }

    fun stopRecording(): Pair<File, Long>? {
        timerJob?.cancel()
        val file = currentOutputFile
        val duration = if (recordingStartTime > 0) System.currentTimeMillis() - recordingStartTime else 0L

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            recordingStartTime = 0L
            recordingDurationSeconds = 0
        }

        return if (file != null && file.exists() && file.length() > 0 && duration > 500) {
            Pair(file, duration)
        } else {
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        timerJob?.cancel()
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        currentOutputFile?.delete()
        cleanup()
    }

    private fun cleanup() {
        mediaRecorder = null
        isRecording = false
        recordingStartTime = 0L
        recordingDurationSeconds = 0
        currentOutputFile = null
    }

    fun fileToBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            "data:audio/m4a;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error encoding file to Base64", e)
            ""
        }
    }

    fun base64ToFile(context: Context, base64String: String, fileName: String = "voice_cache.m4a"): File? {
        return try {
            val clean = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val dir = File(context.cacheDir, "audio_cache").apply { if (!exists()) mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error decoding Base64 to file", e)
            null
        }
    }
}

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    var activePlayingMessageId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playbackProgress by mutableFloatStateOf(0f)
        private set
    var currentPositionSeconds by mutableIntStateOf(0)
        private set

    fun playOrToggle(
        context: Context,
        messageId: String,
        audioSource: String
    ) {
        if (activePlayingMessageId == messageId && isPlaying) {
            pause()
            return
        }

        if (activePlayingMessageId == messageId && mediaPlayer != null) {
            resume()
            return
        }

        stop()
        try {
            val player = MediaPlayer()
            if (audioSource.startsWith("data:audio/")) {
                val file = AudioRecorderHelper.base64ToFile(context, audioSource, "msg_$messageId.m4a")
                if (file != null && file.exists()) {
                    player.setDataSource(file.absolutePath)
                } else {
                    return
                }
            } else if (audioSource.startsWith("content://") || audioSource.startsWith("file://")) {
                player.setDataSource(context, Uri.parse(audioSource))
            } else if (audioSource.startsWith("http://") || audioSource.startsWith("https://")) {
                player.setDataSource(audioSource)
            } else {
                val file = AudioRecorderHelper.base64ToFile(context, audioSource, "msg_$messageId.m4a")
                if (file != null && file.exists()) {
                    player.setDataSource(file.absolutePath)
                } else {
                    return
                }
            }

            player.prepare()
            player.start()

            mediaPlayer = player
            activePlayingMessageId = messageId
            isPlaying = true

            player.setOnCompletionListener {
                stop()
            }

            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio: ${e.message}", e)
            stop()
        }
    }

    private fun startProgressTracker() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isPlaying && mediaPlayer != null) {
                try {
                    val player = mediaPlayer ?: break
                    val pos = player.currentPosition
                    val duration = player.duration
                    if (duration > 0) {
                        playbackProgress = (pos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        currentPositionSeconds = pos / 1000
                    }
                } catch (_: Exception) {
                    break
                }
                delay(100)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
            updateJob?.cancel()
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            startProgressTracker()
        } catch (_: Exception) {}
    }

    fun stop() {
        updateJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        activePlayingMessageId = null
        isPlaying = false
        playbackProgress = 0f
        currentPositionSeconds = 0
    }
}
