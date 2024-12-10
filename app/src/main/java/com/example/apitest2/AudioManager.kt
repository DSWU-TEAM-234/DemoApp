package com.example.apitest2

import android.content.Context
import android.media.MediaPlayer

class AudioManager(context: Context, private val resId: Int) {
    private var mediaPlayer: MediaPlayer? = null

    init {
        // Ensure resId is a valid resource ID (Int)
        mediaPlayer = MediaPlayer.create(context, resId)
    }

    // Play or pause the audio
    fun togglePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
        }
    }

    // Release resources
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Check if the audio is playing
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}
