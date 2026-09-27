package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _maxAmplitude = MutableStateFlow(0)
    val maxAmplitude: StateFlow<Int> = _maxAmplitude.asStateFlow()

    fun startRecording(): File? {
        try {
            val recordingsDir = File(context.cacheDir, "recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()

            val file = File(recordingsDir, "lecture_${System.currentTimeMillis()}.m4a")
            outputFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _isPaused.value = false
            _elapsedSeconds.value = 0

            startTimer()
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error starting recording", e)
            stopRecording()
            return null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && !_isPaused.value) {
            try {
                recorder?.pause()
                _isPaused.value = true
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && _isPaused.value) {
            try {
                recorder?.resume()
                _isPaused.value = false
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recording", e)
            }
        }
    }

    fun stopRecording(): File? {
        timerJob?.cancel()
        timerJob = null

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder", e)
        } finally {
            recorder = null
        }

        _isRecording.value = false
        _isPaused.value = false
        _maxAmplitude.value = 0

        val file = outputFile
        outputFile = null
        return file
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _isRecording.value) {
                delay(100)
                if (!_isPaused.value) {
                    val amp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }
                    _maxAmplitude.value = amp
                }
            }
        }
        scope.launch {
            while (isActive && _isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }
}

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    fun playFile(filePath: String, onCompletion: (() -> Unit)? = null) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                _duration.value = this.duration
                start()
                _isPlaying.value = true

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    progressJob?.cancel()
                    onCompletion?.invoke()
                }
            }
            startProgressTracker()
        } catch (e: IOException) {
            Log.e("AudioPlayerManager", "Failed to play audio", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
        }
    }

    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
        _currentPosition.value = msec
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay(500)
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                    }
                }
            }
        }
    }
}
