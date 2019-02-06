package com.example.meditationtimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.lang.String.format
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class TimerService : Service() {
    companion object {
        val NOTIFY_ID = 1
    }

    inner class TimerBinder : Binder() {
        fun getService() : TimerService = this@TimerService
    }

    private lateinit var notifManager: NotificationManager
    private lateinit var notifBuilder: Notification.Builder
    private var scheduler = Timer()
    lateinit var onTimeChanged : (minutes : Long, seconds : Long) -> Unit
    lateinit var onTimerFinish : () -> Unit

    fun stopTimer() {
        scheduler.cancel()
        scheduler = Timer()
        stopForeground(0)
        notifManager.cancel(NOTIFY_ID)

        onTimerFinish()
    }

    fun startTimer(lengthMinutes: Long, lengthSeconds: Long) {
        val startTime = LocalDateTime.now()
        val endTime = startTime.plusMinutes(lengthMinutes).plusSeconds(lengthSeconds)

        val timerUpdater = object : TimerTask() {
            override fun run() {
                val duration = Duration.between(LocalDateTime.now(), endTime)
                // if there is a nanos component, round up
                val totalSeconds = if (duration.nano > 0) duration.seconds + 1 else duration.seconds
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60

                // if timer is done, clean up
                if (duration.isZero || duration.isNegative) {
                    stopTimer()
                }
                // otherwise update the notification
                else {
                    val timerStr = format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
                    notifBuilder.setContentText(timerStr)
                    notifManager.notify(NOTIFY_ID, notifBuilder.build())
                    onTimeChanged(minutes, seconds)
                }
            }
        }
        startForeground(NOTIFY_ID, notifBuilder.build())
        scheduler.scheduleAtFixedRate(timerUpdater, 0, 1000)
    }

    override fun onCreate() {
        super.onCreate()

        val channelId = "main_channel"
        val channelName = "Main Channel"
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel(channelId, channelName, importance)
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(channel)

        notifBuilder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), 0))
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }
}
