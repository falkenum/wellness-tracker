package com.sjfalken.wellnesstracker

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
        const val NOTIFICATION_ID = 1
    }

    inner class TimerBinder : Binder() {
        fun getService() : TimerService = this@TimerService
    }

    private lateinit var notifManager: NotificationManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var notifBuilder: Notification.Builder
    private lateinit var bellPendingIntent : PendingIntent
    private var scheduler = Timer()
    lateinit var onTimeChanged : (minutes : Long, seconds : Long) -> Unit
    var onTimerFinishTasks = ArrayList<() -> Unit>()

    private lateinit var startTime: ZonedDateTime
    private lateinit var endTime: ZonedDateTime

    var isRunning = false
        private set

    fun stopTimerEarly() {
        // cancel the alarm that was scheduled
        alarmManager.cancel(bellPendingIntent)

        // modify the end instant set by startTimer
        endTime = ZonedDateTime.now()

        stopTimer()
    }

    private fun stopTimer() {
        scheduler.cancel()
        scheduler = Timer()
        stopForeground(0)
        notifManager.cancel(NOTIFICATION_ID)

        isRunning = false

        // save the meditation session
        val record = Entry.newMeditationEntry(startTime, Duration.between(startTime, endTime))
        Thread {
            LogEntryDatabase.instance.entryDao().insert(record)

            // once the record is saved, run all the requested callbacks
            for (task in onTimerFinishTasks) task.invoke()
        }.start()
    }

    fun startTimer(lengthMinutes: Long, lengthSeconds: Long) {
        isRunning = true
        startTime = ZonedDateTime.now()
        endTime = startTime.plusMinutes(lengthMinutes).plusSeconds(lengthSeconds)
        val endTimeMillis = endTime.toEpochSecond() * 1000

        // play the bell now at start
        startService(Intent(applicationContext, BellService::class.java))
        // set alarm to play bell at the end
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, bellPendingIntent)

        val timerUpdater = object : TimerTask() {
            override fun run() {
                val duration = Duration.between(ZonedDateTime.now(), endTime)
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
                    notifBuilder.setContentText("time remaining: " + timerStr)
                    notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
                    onTimeChanged(minutes, seconds)
                }
            }
        }
        startForeground(NOTIFICATION_ID, notifBuilder.build())
        scheduler.scheduleAtFixedRate(timerUpdater, 0, 1000)
    }

    override fun onCreate() {
        super.onCreate()

        val channelId = "timer_channel"
        val channelName = "Timer Channel"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, channelName, importance)
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(channel)

        bellPendingIntent = PendingIntent.getBroadcast(applicationContext, 0,
            Intent(applicationContext, AlarmReceiver::class.java), 0)

        notifBuilder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle("Meditating")
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
