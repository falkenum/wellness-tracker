package com.sjfalken.wellnesstracker

import android.animation.AnimatorInflater
import java.time.*
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.ConditionVariable
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.*
import androidx.navigation.ui.NavigationUI
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import com.google.android.material.tabs.TabLayout
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    class BundleKeys {
        companion object {
            const val REMINDER_TYPE = "com.sjfalken.wellnesstracker.REMINDER_TYPE"
            const val NEW_ENTRY_TYPE = "com.sjfalken.wellnesstracker.ENTRY_TYPE"
            const val REMINDER_ID = "com.sjfalken.wellnesstracker.REMINDER_ID"
        }
    }
    val backupServiceConnectedCV = ConditionVariable()

    var signedInAccount : GoogleSignInAccount? = null
        private set

    lateinit var navController: NavController
        private set

    private val onSignInActions = mutableListOf<(googleAccount : GoogleSignInAccount?) -> Unit>()

    private lateinit var googleSignInClient : GoogleSignInClient
    private var backupService: BackupService? = null
    private val appDataScope = DriveScopes.DRIVE_APPDATA
    private val driveFileScope = DriveScopes.DRIVE


    private val backupServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val backupServiceBinder = (binder as BackupService.BackupBinder)
            backupService = backupServiceBinder.getService()
            backupServiceConnectedCV.open()
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    private companion object {
        const val RC_SIGN_IN = 1
    }

    fun addOnSignInAction(action : (googleAccount : GoogleSignInAccount?) -> Unit) {
        onSignInActions.add(action)
    }

    fun signOut() {
        Log.d("signOut()", "Signing out")
        googleSignInClient.signOut()

        signedInAccount = null

        Toast.makeText(this, "Signed out of account", Toast.LENGTH_SHORT).show()
    }

    fun requestSignIn() {
        googleSignInClient.silentSignIn().addOnSuccessListener { googleAccount ->
            signedInAccount = googleAccount
            onSignedIn()
        }.addOnFailureListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
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

                // using the request code also as a notification filename for the reminder
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

    private fun onDatabaseValidated() {
        setContentView(R.layout.activity_main)

        appBarLayout.stateListAnimator = AnimatorInflater.loadStateListAnimator(this, R.animator.elevated)

        startService(Intent(this, BackupService::class.java))

        setupReminders()

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(appDataScope), Scope(driveFileScope))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
        requestSignIn()


        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        // each fragment tells mainactivity if it wants the type tabs and any menu options for that fragment.
        // mainactivity calls back to fragment before navigation.

        // showing history option only on home page
        navController.addOnDestinationChangedListener { _, _, _ ->
            hideKeyboard()
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.layout_main_drawer)
        NavigationUI.setupWithNavController(toolbar, navController, drawerLayout)

        NavigationUI.setupWithNavController(nav_view, navController)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun onSignedIn() {
        onSignInActions.forEach { it(signedInAccount) }
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(appDataScope, driveFileScope))
        credential.selectedAccount = signedInAccount!!.account
        val googleDriveService : Drive = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential)
            .setApplicationName(getString(R.string.app_name))
            .build()


        backupService!!.init(googleDriveService)
            .addOnSuccessListener {
                Log.d("onActivityResult()", "Initialized backup service")
            }.addOnFailureListener {

                it.printStackTrace()

                Utility.ErrorDialogFragment().apply {
                    message = "Failed to initialize backup service"
                }.show(supportFragmentManager, null)
            }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> {
                GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener { account ->
                    signedInAccount = account
                    onSignedIn()
                }.addOnFailureListener {
                    Utility.InfoDialogFragment().apply {
                        message = "Account not logged in, cannot sync database"
                    }.show(supportFragmentManager, null)
                }
            }
        }
    }


    fun doSync() {
        backupService!!.syncDatabaseFiles().addOnSuccessListener {
            Toast.makeText(this@MainActivity,
                "Synced local database with Google Drive", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Utility.ErrorDialogFragment().apply {
                message = "Failed to sync local database with Google Drive"
            }.show(supportFragmentManager, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backupServiceIntent = Intent(applicationContext, BackupService::class.java)
        bindService(backupServiceIntent, backupServiceConnection, Context.BIND_AUTO_CREATE)

        Thread {
            LogEntryDatabase.init(this, LogEntryDatabase.DB_NAME_PRIMARY)

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
            backupServiceConnectedCV.block()
            runOnUiThread {
                onDatabaseValidated()
            }

        }.start()


    }

    override fun onDestroy() {
        unbindService(backupServiceConnection)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val entryType = intent?.getStringExtra(BundleKeys.NEW_ENTRY_TYPE)

        if (entryType != null) {
            val args = Bundle().apply {
                putString(NewEntryFragment.ArgumentKeys.NEW_ENTRY_TYPE, entryType)
            }
            navController.navigate(R.id.newEntryFragment, args, null)
        }
    }

}

