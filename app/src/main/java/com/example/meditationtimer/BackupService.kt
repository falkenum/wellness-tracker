package com.example.meditationtimer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.meditationtimer.BackupService.Companion.REMOTE_ROOT
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2 as usingOAuth21

class FileSyncHelper(private val googleDriveService : Drive, val localSyncDir : java.io.File) {
    companion object {
        const val BACKUP_FOLDER_NAME = "WellnessTracker.backup"
    }

    val backupFolderMetadata : File = listRemoteFiles().find { it.name == BACKUP_FOLDER_NAME } ?: run {
            val newBackupFolderMetadata = File().apply {
                name = BACKUP_FOLDER_NAME
                val folderMimeType = "application/vnd.google-apps.folder"
                mimeType = folderMimeType
            }

            // create the new folder and store its new metadata from the server
            googleDriveService.files().create(newBackupFolderMetadata).apply { fields = "id" }.execute()
        }

    fun listRemoteFiles() : List<File> {
        return googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(REMOTE_ROOT).execute().files
    }

    private fun updateRemoteWithLocal(remoteFileMetadata : File?, localFile : java.io.File) {
        val remoteExists = remoteFileMetadata != null

        val content = StringBuilder().run {
            localFile.bufferedReader().lines().forEach { append(it).append('\n') }
            toString()
        }

        val contentStream = ByteArrayContent.fromString(null, content)


        googleDriveService.files().run {
            if (remoteExists) {
                // either get remote data or push local data

//                Log.d(tag, "updating remote file ${localFile.name}")
                update(remoteFileMetadata!!.id, null, contentStream)
            }
            else {
                val metadata = File().apply {
                    name = localFile.name

                    // set the main backup folder as its parent
                    parents = mutableListOf(backupFolderMetadata.id)
                }

//                Log.d(tag, "creating remote file ${localFile.name}")
                create(metadata, contentStream)
            }
        }.execute()
    }

    private fun updateLocalWithRemote(localFile : java.io.File?, remoteFileMetadata : File) {
        val bytes = googleDriveService.files().get(remoteFileMetadata.id).executeAsInputStream().readBytes()

        if (localFile != null) {
            localFile.bufferedWriter().append(bytes.contentToString())
        }
        else {
            // create the file with the known root
            val newFile = java.io.File(localSyncDir.absolutePath + remoteFileMetadata.name)
            newFile.bufferedWriter().append(bytes.contentToString())
        }

    }

    fun syncLocalAndRemote(localFile : java.io.File?, remoteFileMetadata : File?) {

        val tag = "syncLocalAndRemote()"

        when (Pair(localFile == null, remoteFileMetadata == null)) {

            // update older with newer
            Pair(false, false) -> {
                val localLastModified = localFile!!.lastModified()
                val remoteLastModified = remoteFileMetadata!!.modifiedTime.value

                // if local file is newer
                if (localLastModified >= remoteLastModified) {
                    Log.d(tag, "updating remote with local data")
                    updateRemoteWithLocal(remoteFileMetadata, localFile)
                }
                else {
                    Log.d(tag, "updating local with remote data")
                    updateLocalWithRemote(localFile, remoteFileMetadata)
                }
            }

            // copy local to remote
            Pair(false, true) -> updateRemoteWithLocal(remoteFileMetadata, localFile!!)
            // copy remote to local
            Pair(true, false) -> updateLocalWithRemote(localFile, remoteFileMetadata!!)
        }

    }
}

class BackupService : Service() {
    companion object {
        const val REMOTE_ROOT = "drive"
    }

    private val executor = Executors.newSingleThreadExecutor()


//    fun syncDatabaseFiles(credential: GoogleAccountCredential) {
//        // Use the authenticated account to sign in to the Drive service.
//
//
//    }

    fun syncDatabaseFiles(credential: GoogleAccountCredential) : Task<Any> {
        return Tasks.call (executor, Callable {
            val googleDriveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                credential)
                .setApplicationName(getString(R.string.app_name))
                .build()

            val localSyncDir = filesDir.parentFile.listFiles().find { file -> file.name == "databases" }!!
            val fileSyncHelper = FileSyncHelper(googleDriveService, localSyncDir)
            //            googleDriveService.files().emptyTrash().execute()

            // find the databases directory

            val filesToBackup = listOf(
                    LogEntryDatabase.DB_NAME,
                    "${LogEntryDatabase.DB_NAME}-shm",
                    "${LogEntryDatabase.DB_NAME}-wal"
                )

            val tag = "syncDataBaseFile()"

            Log.d(tag, "preparing to sync...")

            val remoteFiles = fileSyncHelper.listRemoteFiles()

            Log.d(tag, "Number of remote files: ${remoteFiles.size}")

            remoteFiles.forEach { file ->
                Log.d(tag, "File ${file.name} parents: ${file.parents}")
            }

            for (filename in filesToBackup) {
                val localFile = fileSyncHelper.localSyncDir.listFiles().find { it.name == filename }

                val remoteFileMetadata = remoteFiles.find { remoteFile ->
                    // the false means to not include this file if it has no parents

                    val nameMatches = remoteFile.name == filename
                    val parentMatches =
                        remoteFile.parents?.contains(fileSyncHelper.backupFolderMetadata.id) ?: false

                    nameMatches && parentMatches
                }


                val debugStr = StringBuilder().run {
                    append("\nfilename: $filename")
                    append("\nlocal exists: ${localFile != null}")
                    append("\nremote exists: ${remoteFileMetadata != null}")
//                    append("\nbackup's parents : ${backup?.parents ?: "null"}")

                    toString()
                }

                Log.d(tag, debugStr)

                fileSyncHelper.syncLocalAndRemote(localFile, remoteFileMetadata)
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
