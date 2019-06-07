package com.example.meditationtimer

import android.accounts.Account
import java.time.*
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.*
import androidx.navigation.ui.NavigationUI
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*

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

    override fun onTabSelected(tab: TabLayout.Tab) {
        selectedType = tab.text.toString()
        onTabSelectedActions.forEach { onTabSelectedAction -> onTabSelectedAction(tab) }
    }

    private lateinit var navController: NavController

    var selectedType = EntryTypes.getTypes()[0]

    private val onTabSelectedActions = mutableListOf<(TabLayout.Tab) -> Unit>()
    private val fragmentsToShowTabs = listOf(R.id.homeFragment, R.id.newEntryFragment)
    private val scope =  Scope(Scopes.DRIVE_APPFOLDER)
    private companion object {
        const val RC_SIGN_IN = 1
    }

    fun addOnTabSelectedAction(action : (TabLayout.Tab) -> Unit) {
        onTabSelectedActions.add(action)
    }

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

    private fun updateHistoryButton(fadeIn : Boolean) {
        val fade = Fade(if (fadeIn) Fade.IN else Fade.OUT).apply {
            // 400 ms transition
            this.duration = 400
        }

        TransitionManager.beginDelayedTransition(toolbar, fade)

        val historyActionButton = findViewById<View>(R.id.historyActionButton)
        historyActionButton.visibility = if (fadeIn) View.VISIBLE else View.GONE

    }

    private fun changeTabDrawer(open : Boolean) {

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        tabLayout.visibility = if (open) View.VISIBLE else View.GONE
    }

    private fun onDatabaseLoaded() {
        setContentView(R.layout.activity_main)
        findViewById<TabLayout>(R.id.tabLayout)!!.run {
            EntryTypes.getTypes().forEach { addTab(newTab().setText(it)) }
            addOnTabSelectedListener(this@MainActivity)
        }

        // these are creating services
        startService(Intent(this, TimerService::class.java))
        startService(Intent(this, BackupService::class.java))

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

        // each fragment tells mainactivity if it wants the type tabs and any menu options for that fragment.
        // mainactivity calls back to fragment before navigation.

        // showing history option only on home page
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // if any fragment has requested to show some options on the toolbar, then show them
            // TODO
            val showHistoryButton = when (destination.id) {
                R.id.homeFragment -> true
                else -> false
            }
            updateHistoryButton(showHistoryButton)
            updateTabLayout(destination)
            hideKeyboard()
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.layout_main_drawer)
        NavigationUI.setupWithNavController(toolbar, navController, drawerLayout)

        val drawerContent = findViewById<NavigationView>(R.id.view_drawer_content)
        NavigationUI.setupWithNavController(drawerContent, navController)

        changeTabDrawer(true)
    }

    private fun updateTabLayout(destination : NavDestination) {
        // if any fragment has requested tabLayout, then show it
        val showTabLayout = fragmentsToShowTabs.any { it == destination.id }
        changeTabDrawer(showTabLayout)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
            account?.run { makeRequest(this) }
        }
    }

    private fun doNetworkTasks() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(scope)
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) startActivityForResult(googleClient.signInIntent, RC_SIGN_IN)
        else makeRequest(account)

    }

    private fun makeRequest(googleAccount : GoogleSignInAccount) {

        val token = GoogleAuthUtil.getToken(this, googleAccount.account, scope.scopeUri)
        val url = "https://www.googleapis.com/drive/v2/about"

        val auth = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }

        }

        val httpClient = OkHttpClient.Builder()
            .authenticator(auth)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            val response = httpClient.newCall(request).execute()
            Log.d("debugging", response.body!!.string())
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        doNetworkTasks()

        Thread {
            LogEntryDatabase.init(this)

            val entryDao = LogEntryDatabase.instance.entryDao()
            val entries = entryDao.getAll()

            val invalidEntries = mutableListOf<Entry>()

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

