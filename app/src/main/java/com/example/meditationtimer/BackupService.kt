package com.example.meditationtimer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.*
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.security.KeyFactorySpi
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext.fold
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2 as usingOAuth21

class BackupService : Service() {
    companion object {
        const val REMOTE_ROOT = "drive"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var googleDriveService : Drive


    fun doDriveTasks(credential: GoogleAccountCredential) {
        // Use the authenticated account to sign in to the Drive service.
        googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential)
            .setApplicationName(getString(R.string.app_name))
            .build()


        syncDatabaseFiles().apply {
            addOnSuccessListener {
//                Toast.makeText(,
//                    "Synced local database with Google Drive", Toast.LENGTH_SHORT).show()
                //TODO record success datetime in database
            }

            addOnFailureListener {e ->
                val errorMessage = "Failed to sync with remote database"
                val tag = "doDriveTasks()"

//                Utility.ErrorDialogFragment().apply {
//                    message = errorMessage
//                }.show(supportFragmentManager, null)

                //TODO record error datetime in database

                val stackTraceStr = e.stackTrace.run {
                    fold("$e\n") { accString, elt ->
                        accString.plus("$elt\n")
                    }
                }

                Log.e(tag, "$errorMessage: due to... \n$stackTraceStr")
            }
        }
    }

    private fun syncDatabaseFiles() : Task<Any> {
        return Tasks.call (executor, Callable {
            //            googleDriveService.files().emptyTrash().execute()

            // find the databases directory
            val localDir = filesDir.parentFile.listFiles().find { file -> file.name == "databases" }
            val tag = "syncDataBaseFile()"

            Log.d(tag, "preparing to sync...")

            val remoteFiles = googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(REMOTE_ROOT).execute().files
//                .filter { file -> file.name.contains(LogEntryDatabase.DB_NAME) }

            val backupFolderName = "WellnessTracker.backup"
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

    override fun onBind(intent: Intent): IBinder {
        return BackupBinder()
    }

    inner class BackupBinder : Binder() {
        fun getService() : BackupService = this@BackupService
    }

}
