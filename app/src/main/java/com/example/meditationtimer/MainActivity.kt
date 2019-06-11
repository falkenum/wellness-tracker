package com.example.meditationtimer

import java.time.*
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.lang.StringBuilder
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class BundleKeys {
    companion object {
        const val REMINDER_TYPE = "reminder type"
        const val REMINDER_ID = "reminder id"
    }
}

class MainActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener {

    var selectedType = EntryTypes.getTypes()[0]
    private lateinit var navController: NavController

    private val onTabSelectedActions = mutableListOf<(TabLayout.Tab) -> Unit>()
    private val fragmentsToShowTabs = listOf(R.id.homeFragment, R.id.newEntryFragment)
    private val executor = Executors.newSingleThreadExecutor()
    private val appDataScope = DriveScopes.DRIVE_APPDATA
    private val driveFileScope = DriveScopes.DRIVE_FILE
    private lateinit var googleSignInClient : GoogleSignInClient


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

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(appDataScope), Scope(driveFileScope))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        navController = findNavController(R.id.nav_host_fragment)

        val tag = "onDatabaseLoaded()"
        backupButton.setOnClickListener {
            Log.d(tag, "Backup button pressed")
            requestSignIn()
        }

        signOutButton.setOnClickListener {
            Log.d(tag, "Signing out")
            googleSignInClient.signOut()

            Toast.makeText(this, "Signed out out of account", Toast.LENGTH_SHORT).show()
        }

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

    private fun requestSignIn() {
//        client.revokeAccess()
//        client.signOut()

        val googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        Log.d("requestSignIn()", "signed in account: ${googleAccount?.email ?: "null"}")

        // The result of the sign-in Intent is handled in onActivityResult.
//        if (googleAccount == null) startActivityForResult(client.signInIntent, RC_SIGN_IN)
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)

        // if already signed in, use the network
//        else doDriveTasks(googleAccount)
    }

    private fun doDriveTasks(googleAccount : GoogleSignInAccount) {
        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(appDataScope, driveFileScope))
        credential.selectedAccount = googleAccount.account
        val googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential)
            .setApplicationName(getString(R.string.app_name))
            .build()


        syncDatabaseFiles(googleDriveService).apply {
            addOnSuccessListener {
                Toast.makeText(this@MainActivity,
                    "Synced local database with Google Drive", Toast.LENGTH_SHORT).show()
            }

            addOnFailureListener {e ->
                val errorMessage = "Failed to sync with remote database"
                val tag = "doDriveTasks()"

                Utility.ErrorDialogFragment().apply {
                    message = errorMessage
                }.show(supportFragmentManager, null)

                val stackTraceStr = e.stackTrace.run {
                    fold("$e\n") { accString, elt ->
                        accString.plus("$elt\n")
                    }
                }

                Log.e(tag, "$errorMessage: due to... \n$stackTraceStr")
            }
        }
    }

    private fun syncDatabaseFiles(googleDriveService : Drive) : Task<Any> {
        return Tasks.call (executor, Callable {
//            googleDriveService.files().emptyTrash().execute()

            // find the databases directory
            val localDir = filesDir.parentFile.listFiles().find { file -> file.name == "databases" }
            val tag = "syncDataBaseFile()"

            Log.d(tag, "preparing to sync...")

            val backupFolderName = "WellnessTracker.backup"
            val remoteRoot = "drive"
            val remoteFiles = googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(remoteRoot).execute().files
//                .filter { file -> file.name.contains(LogEntryDatabase.DB_NAME) }

            // find backup folder or create it
            val backupFolderMetadata : File = remoteFiles.find { it.name == backupFolderName } ?: run {
                val newBackupFolderMetadata = File().apply {
                    name = backupFolderName
                    val folderMimeType = "application/vnd.google-apps.folder"
                    mimeType = folderMimeType
                }

                Log.d(tag, "Creating new backup folder on remote")
                // create the new folder and store its new metadata from the server
                googleDriveService.files().create(newBackupFolderMetadata).apply { fields = "id" }.execute()
            }

            Log.d(tag, "Number of remote files: ${remoteFiles.size}")

//            var numFilesWithParent = 0
            remoteFiles.forEach { file ->
                Log.d(tag, "File ${file.name} parents: ${file.parents}")


//                googleDriveService.files().delete(file.id).execute()
//                Log.d(tag, "deleted file ${file.name}")

//                file.parents?.let { parents ->
//                    numFilesWithParent++
//                    if (parents[0] == backupFolderMetadata.id) {
//                    }
//                }
            }

//            Log.d(tag, "$numFilesWithParent files have at least one parent")

            for (localFile in localDir!!.listFiles()) {

                val content = StringBuilder().run {
                    localFile.bufferedReader().lines().forEach { append(it).append('\n') }
                    toString()
                }

                val contentStream = ByteArrayContent.fromString(null, content)

                val remoteFileMetadata = remoteFiles.find { remoteFile ->
                    // the false means to not include this file if it has no parents

                    val nameMatches = remoteFile.name == localFile.name
                    val parentMatches = remoteFile.parents?.contains(backupFolderMetadata.id) ?: false

                    nameMatches && parentMatches
                }
                val fileHasBackup = remoteFileMetadata != null

                val debugStr = StringBuilder().run {
                    append("\nlocal file found")
                    append("\nname: ${localFile.name}")
                    append("\nhas backup: $fileHasBackup")
//                    append("\nbackup's parents : ${backup?.parents ?: "null"}")

                    toString()
                }

                Log.d(tag, debugStr)

                googleDriveService.files().run {
                    if (fileHasBackup) {
                        Log.d(tag, "updating remote file ${localFile.name}")
                        update(remoteFileMetadata!!.id, null, contentStream)
                    }
                    else {
                        val metadata = File().apply {
                            name = localFile.name

                            // set the main backup folder as its parent
                            parents = mutableListOf(backupFolderMetadata.id)
                        }

                        Log.d(tag, "creating remote file ${localFile.name}")
                        create(metadata, contentStream)
                    }
                }.execute()
            }

            Any()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).result

                if (account != null) doDriveTasks(account)
                else Utility.InfoDialogFragment().apply {
                    message = "Account not logged in, cannot sync database"
                }.show(supportFragmentManager, null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        selectedType = tab.text.toString()
        onTabSelectedActions.forEach { onTabSelectedAction -> onTabSelectedAction(tab) }
    }
}

