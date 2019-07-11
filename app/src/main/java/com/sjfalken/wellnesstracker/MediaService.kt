package com.sjfalken.wellnesstracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File

interface MediaAccessor {
    fun getMediaDir(context: Context) : File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path)
            .apply { if (!exists()) mkdir() }
    }

}

class MediaService : Service(), MediaAccessor {
    companion object {
        const val ARG_FILENAME = "com.sjfalken.wellnesstracker.ARG_FILENAME"
        const val NOTIFICATION_ID = 0
    }
    private var mediaPlayer: MediaPlayer? = null
    lateinit var notificationManager : NotificationManager
    lateinit var notificationBuilder : Notification.Builder
    override fun onCreate() {
        super.onCreate()
        val channelId = "media_channel"
        val channelName = "Media Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        notificationBuilder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_media)
            .setContentTitle("Media playing")
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), 0))
    }

    private fun stopMedia() {
        mediaPlayer?.run { stop(); release() }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer = null
        }

        val filename = intent?.extras?.getString(ARG_FILENAME)!!
        val mediaFile = File("${getMediaDir(this).path}/$filename")

        Log.d("onStartcommand()", mediaFile.path)
        Log.d("onStartcommand()", mediaFile.exists().toString())

        mediaPlayer = MediaPlayer.create(applicationContext, Uri.fromFile(mediaFile))
        val mediaPlayer = mediaPlayer!!

        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            stopSelf()
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

        mediaPlayer.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopMedia()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
