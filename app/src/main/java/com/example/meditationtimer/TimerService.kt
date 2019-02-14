package com.example.meditationtimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.lang.String.format
import java.time.*
import java.util.*

class TimerService : Service() {
    companion object {
        val NOTIFY_ID = 1
    }

    inner class TimerBinder : Binder() {
        fun getService() : TimerService = this@TimerService
    }

    private lateinit var notifManager: NotificationManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var notifBuilder: Notification.Builder
    private lateinit var bellIntent : PendingIntent
    private var scheduler = Timer()
    lateinit var onTimeChanged : (minutes : Long, seconds : Long) -> Unit
    lateinit var onTimerFinish : () -> Unit

    var isRunning = false
        private set

    fun stopTimerEarly() {
        // cancel the alarm that was scheduled
        alarmManager.cancel(bellIntent)

        stopTimer()
    }

    private fun stopTimer() {
        scheduler.cancel()
        scheduler = Timer()
        stopForeground(0)
        notifManager.cancel(NOTIFY_ID)

        onTimerFinish()

        isRunning = false
    }

    fun startTimer(lengthMinutes: Long, lengthSeconds: Long) {
        isRunning = true
        val startTime = OffsetDateTime.now()
        val endTime = startTime.plusMinutes(lengthMinutes).plusSeconds(lengthSeconds)
        val endTimeMillis = endTime.toEpochSecond() * 1000

        // play the bell now at start
        startService(Intent(applicationContext, BellService::class.java))
        // set alarm to play bell at the end
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimeMillis, bellIntent)

        val timerUpdater = object : TimerTask() {
            override fun run() {
                val duration = Duration.between(OffsetDateTime.now(), endTime)
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

        bellIntent = PendingIntent.getService(applicationContext, 0,
            Intent(applicationContext, BellService::class.java), 0)

        notifBuilder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentIntent(
                PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), 0))


        // alarm to play the bell at the end
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }
}