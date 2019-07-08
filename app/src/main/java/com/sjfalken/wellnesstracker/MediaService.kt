package com.sjfalken.wellnesstracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class MediaService : Service() {
    companion object {
        const val ARG_PLAYBACK_ID = "com.sjfalken.wellnesstracker.ARG_PLAYBACK_ID"
        const val NOTIFICATION_ID = 0
    }
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val id = intent?.extras?.getInt(ARG_PLAYBACK_ID)!!

        mediaPlayer = MediaPlayer.create(this, id)

        val mediaPlayer = mediaPlayer!!

        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            stopSelf()
        }


        val channelId = "media_channel"
        val channelName = "Media Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        val notificationBuilder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_media)
            .setContentTitle("Media playing")
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), 0))

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        // make sure to start from the beginning
        mediaPlayer.seekTo(0)
        mediaPlayer.start()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
