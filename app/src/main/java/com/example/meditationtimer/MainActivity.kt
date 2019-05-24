package com.example.meditationtimer

import android.animation.ObjectAnimator
import java.time.*
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView

class BundleKeys {
    companion object {
        const val TIMER_SERVICE_BINDER = "timer service binder"
        const val REMINDER_TYPE = "reminder type"
        const val REMINDER_ID = "reminder id"
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

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

        setContentView(R.layout.activity_main)

        // this is creating the service if it does not exist
        startService(Intent(this, TimerService::class.java))

        Thread {
            RecordDatabase.init(this)
        }.start()

        setupReminders()

        navController = findNavController(R.id.nav_host_fragment)

        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.menu_options)

            // only one menu item currently
            setOnMenuItemClickListener {
                navController.navigate(R.id.historyFragment)
                true
            }
        }

        // showing history option only on home page
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val opaque = 1f
            val transparent = 0f

            val (alphaStart, alphaEnd) = when (destination.id) {
                R.id.homeFragment -> Pair(transparent, opaque)
                else -> Pair(opaque, transparent)
            }

            // fade in or fade out the button
            findViewById<View>(R.id.historyActionButton).let {
                ObjectAnimator.ofFloat(it, "alpha", alphaStart, alphaEnd).start()
            }

        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.layout_main_drawer)
        NavigationUI.setupWithNavController(toolbar, navController, drawerLayout)

        val drawerContent = findViewById<NavigationView>(R.id.view_drawer_content)
        NavigationUI.setupWithNavController(drawerContent, navController)

    }
}

