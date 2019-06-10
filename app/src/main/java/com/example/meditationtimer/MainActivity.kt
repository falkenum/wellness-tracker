package com.example.meditationtimer

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
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

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
            // TODO if any fragment has requested to show some options on the toolbar, then show them
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

    private val driveScope = DriveScopes.DRIVE

    private fun requestSignIn() : GoogleSignInAccount? {

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope(driveScope))
                        .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        var googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        Log.d("requestSignIn: googleAccount", "${googleAccount == null}.")
        // The result of the sign-in Intent is handled in onActivityResult.
        if (googleAccount == null) {
            startActivityForResult(client.signInIntent, RC_SIGN_IN)
            googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        }

        return googleAccount
    }

    private fun doDriveTasks(googleAccount : GoogleSignInAccount) {
        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(driveScope))
        credential.selectedAccount = googleAccount.getAccount()
        val googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential)
            .setApplicationName(getString(R.string.app_name))
            .build()


        syncDatabaseFiles(googleDriveService).apply {
            addOnFailureListener {e ->
                val errorMessage = "Failed to sync with remote database"
                val tag = "doDriveTasks()"
                Log.e(tag, errorMessage )
                Log.e(tag, e.toString() )

                Utility.ErrorDialogFragment().apply {
                    message = errorMessage
                }.show(supportFragmentManager, null)
            }

        }

    }

    private fun syncDatabaseFiles(googleDriveService : Drive) : Task<Any> {
        return Tasks.call {
            // check local and remote file status, exists or not, which is newer.
            // copy newer to null or older side

//            val remoteDir = googleDriveService.files().list().setSpaces("appDataFolder").execute().files
//            val remoteLastUpdatedTime = remoteDir.find { file -> file.name == LogEntryDatabase.DB_NAME }?.modifiedTime
//
            // find the databases directory
            val localDir = filesDir.parentFile.listFiles().find { file -> file.name == "databases" }


            for (file in localDir!!.listFiles()) {
                Log.d("syncDataBaseFile", file.absolutePath)

//                googleDriveService.files().update()
//                Log.d("syncDataBaseFile", file.canonicalPath)
            }

//            val localLastUpdated
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = requestSignIn()

        if (account != null) doDriveTasks(account)

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
                Utility.DebugDialogFragment().apply {
                    message = "Deleted ${invalidEntries.size} invalid entries: "
                    for (entry in invalidEntries) {
                        message = message + "; " + entry.toString()
                    }
                }.show(supportFragmentManager, null)
            }

            runOnUiThread {
                onDatabaseLoaded()
            }

        }.start()


    }
}

