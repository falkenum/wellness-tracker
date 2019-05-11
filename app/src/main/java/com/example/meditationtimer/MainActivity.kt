package com.example.meditationtimer

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import java.time.*

class BundleKeys {
    companion object {
        const val TIMER_SERVICE_BINDER = "timer service binder"
        const val REMINDER_TYPE = "reminder type"
        const val REMINDER_ID = "reminder id"
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var timerServiceIntent: Intent

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)
            val viewPager = findViewById<ViewPager>(R.id.viewPager)

            // once the service is connected, setup the tabs
            viewPager.adapter = MainPagerAdapter(supportFragmentManager, timerServiceBinder)
            findViewById<TabLayout>(R.id.tabLayout).setupWithViewPager(viewPager)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun setupReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var requestCode = 0
        for (type in RecordTypes.getTypes()) {
            val times = RecordTypes.getConfig(type).getDailyReminderTimes()
            val receiverIntent = Intent(applicationContext, ReminderReceiver::class.java)
                .putExtra(BundleKeys.REMINDER_TYPE, type)

            // if there are reminders for this config type, then make a pending intent for each one
            if (times != null) for (time in times) {

                // using the request code also as a notification id for the reminder
                receiverIntent.putExtra(BundleKeys.REMINDER_ID, requestCode)
                // need a different request code for every alarm set
                val receiverPendingIntent = PendingIntent.getBroadcast(applicationContext, requestCode,
                    receiverIntent, 0)
                requestCode++

                var firstAlarmTime = LocalDateTime.of(LocalDate.now(), time)

                // if the reminder for this day has already passed, then start it tomorrow
                if (firstAlarmTime.isBefore(LocalDateTime.now()))
                    firstAlarmTime = firstAlarmTime.plusDays(1 )


                val firstAlarmTimeMillis = ZonedDateTime.of(firstAlarmTime, ZoneId.systemDefault())
                    .toEpochSecond() * 1000

                // interval in ms
                val millisInDay : Long = 1000*60*60*24
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    firstAlarmTimeMillis, millisInDay, receiverPendingIntent)
            }
        }



    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendBroadcast(Intent(applicationContext, ReminderReceiver::class.java))

        setContentView(R.layout.activity_main)
        timerServiceIntent = Intent(this, TimerService::class.java)

        // this is creating the service if it does not exist
        startService(timerServiceIntent)

        Thread {
            RecordDatabase.init(this)
            // this is creating a connection to the service
            // wait for database to init first
            bindService(timerServiceIntent, timerConnection, 0)
        }.start()

        setupReminders()
    }

    override fun onDestroy() {
        unbindService(timerConnection)
        super.onDestroy()
    }
}

