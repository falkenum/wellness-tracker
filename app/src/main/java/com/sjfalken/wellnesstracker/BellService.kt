package com.sjfalken.wellnesstracker

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class BellService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.bell)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // make sure to start from the beginning
        mediaPlayer.seekTo(0)
        mediaPlayer.start()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
