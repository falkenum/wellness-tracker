package com.example.meditationtimer

import android.animation.LayoutTransition
import java.time.*
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.transition.Scene
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*

class BundleKeys {
    companion object {
        const val TIMER_SERVICE_BINDER = "timer service binder"
        const val REMINDER_TYPE = "reminder type"
        const val REMINDER_ID = "reminder id"
    }
}

class MainActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        onTabSelectedActions.forEach { onTabSelectedAction -> onTabSelectedAction(tab!!) }
    }

    private lateinit var navController: NavController

    private fun setupReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var requestCode = 0
        for (type in EntryTypes.getTypes()) {
            val times = EntryTypes.getConfig(type).getDailyReminderTimes()
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

   val selectedType : String
        get() {
            return findViewById<TabLayout>(R.id.tabLayout).run {
                getTabAt(selectedTabPosition)!!.text.toString()
            }
        }

    val onTabSelectedActions = MutableList(0) { { tab : TabLayout.Tab -> } }
    fun addOnTabSelectedAction(onTabSelectedAction : (TabLayout.Tab) -> Unit) {
        onTabSelectedActions.add(onTabSelectedAction)
    }

    private fun fadeHistoryButton(fadeIn : Boolean) {
        val fade = Fade(if (fadeIn) Fade.IN else Fade.OUT).apply {
            // 400 ms transition
            this.duration = 400
        }

        TransitionManager.beginDelayedTransition(toolbar, fade)

        val historyActionButton = findViewById<View>(R.id.historyActionButton)
        historyActionButton.visibility = if (fadeIn) View.VISIBLE else View.INVISIBLE
    }

    private fun changeTabDrawer(open : Boolean) {
        val slide = Slide().apply {
            slideEdge = Gravity.TOP
            duration = 400
            mode = if (open) Slide.MODE_IN else Slide.MODE_OUT
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.visibility = if (open) View.VISIBLE else View.GONE
//        tabLayoutHolder.layoutTransition = LayoutTransition().apply {
//            set
//        }
//        val fade = Fade().apply {
//            // 400 ms transition
//            duration = 400
//            addTarget(tabLayout)
//        }
//
//        val endScene = Scene(tabLayoutHolder, tabLayout)
//        TransitionManager.go(endScene, fade)

//        TransitionManager.beginDelayedTransition(tabLayoutHolder, slide)
//        tabLayout.visibility = if (open) View.VISIBLE else View.GONE

    }

    private fun onDatabaseLoaded() {
        setContentView(R.layout.activity_main)
        findViewById<TabLayout>(R.id.tabLayout)!!.run {
            for (type in EntryTypes.getTypes()) {
                addTab(newTab().setText(type))
            }

            addOnTabSelectedListener(this@MainActivity)
        }


        // this is creating the service if it does not exist
        startService(Intent(this, TimerService::class.java))

        setupReminders()

        navController = findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.homeFragment)
        navController.graph.startDestination = R.id.homeFragment

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
            val showHistoryButton = when (destination.id) {
                R.id.homeFragment -> true
                else -> false
            }

            val showTabLayout = when (destination.id) {
                R.id.newEntryFragment -> true
                R.id.homeFragment -> true
                else -> false
            }

            fadeHistoryButton(showHistoryButton)
            changeTabDrawer(showTabLayout)

            hideKeyboard()
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.layout_main_drawer)
        NavigationUI.setupWithNavController(toolbar, navController, drawerLayout)

        val drawerContent = findViewById<NavigationView>(R.id.view_drawer_content)
        NavigationUI.setupWithNavController(drawerContent, navController)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread {
            LogEntryDatabase.init(this)

            val entryDao = LogEntryDatabase.instance.entryDao()
            val entries = entryDao.getAll()

            // I just wanted an empty mutable list
            val invalidEntries = entries.filter { false }.toMutableList()

            for (entry in entries)
                if (!Entry.isValidEntry(entry)) {
                    invalidEntries.add(entry)
                    entryDao.delete(entry)
                }

            if (invalidEntries.size > 0) {
                DebugDialogFragment().apply {
                    message = "Deleted ${invalidEntries.size} invalid entries: "
                    for (entry in invalidEntries) {
                        message = message + "; " + entry.toString()
                    }
                }.show(supportFragmentManager, "DebugDialog")
            }

            runOnUiThread {
                onDatabaseLoaded()
            }

        }.start()


    }
}

