package com.example.meditationtimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.telecom.Call
import android.util.Log
import com.example.meditationtimer.BackupService.Companion.REMOTE_FS
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

class FileSyncHelper(private val googleDriveService : Drive, context: Context) {
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
    private val localSyncDir = context.filesDir.parentFile.listFiles().find { file -> file.name == "databases" }!!

    fun listRemoteFiles() : List<File> {
        return googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(REMOTE_FS).execute().files
    }

    fun updateRemoteWithLocal(remoteFileMetadata : File?, localFile : java.io.File) {
        val remoteExists = remoteFileMetadata != null

        val content = StringBuilder().run {
            localFile.readLines().forEach { append(it).append('\n') }
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

    fun updateLocalWithRemote(localFile : java.io.File?, remoteFileMetadata : File) {
        val bytes = googleDriveService.files().get(remoteFileMetadata.id).executeAsInputStream().readBytes()

        if (localFile != null) {
            localFile
        }
        else {
            // create the file with the known root
            val newFile = java.io.File(localSyncDir.absolutePath + remoteFileMetadata.name)
            newFile
        }.writeBytes(bytes)

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
                    Log.d(tag, "local is newer; updating remote with local data")
                    updateRemoteWithLocal(remoteFileMetadata, localFile)
                }
                else {
                    Log.d(tag, "remote is newer; updating local with remote data")
                    updateLocalWithRemote(localFile, remoteFileMetadata)
                }
            }

            // copy local to remote
            Pair(false, true) -> updateRemoteWithLocal(remoteFileMetadata, localFile!!)
            // copy remote to local
            Pair(true, false) -> updateLocalWithRemote(localFile, remoteFileMetadata!!)
        }

    }

//    fun restoreDatabase() {
//    }
//
//    fun backupDatabase() {
//    }
}

class BackupService : Service() {
    companion object {
        const val REMOTE_FS = "drive"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var localFile : java.io.File? = null
    private var fileSyncHelper : FileSyncHelper? = null

    private var googleDriveService : Drive? = null
        set(value) {

            if (value == null) {
                field = null
                fileSyncHelper = null
                return
            }

            fileSyncHelper = FileSyncHelper(value, this)

            field = value
        }

    fun init(googleDriveService: Drive) : Task<BackupService> {
        return Tasks.call(executor, Callable {
            localFile = java.io.File("${filesDir.absolutePath}/databases/${LogEntryDatabase.DB_NAME}")

            this.googleDriveService = googleDriveService

            this@BackupService
       })
    }

    fun backupDatabaseFiles() : Task<Unit> {
        return Tasks.call (executor, Callable {

            // will overwrite remote db with local
            fileSyncHelper!!.syncLocalAndRemote(localFile, null)
        })
    }

    fun syncDatabaseFiles() : Task<Unit> {
        return Tasks.call (executor, Callable {

            // wal checkpoint for the SQLite database
            // basically preparing the database to be synced
            // TODO this won't work because local database is always newer. need to merge entries from each db
            LogEntryDatabase.checkpoint()

            val tag = "syncDataBaseFile()"

            Log.d(tag, "preparing to sync...")

            val remoteFiles = fileSyncHelper!!.listRemoteFiles()

            Log.d(tag, "Number of remote files: ${remoteFiles.size}")

            val remoteFileMetadata = remoteFiles.find { remoteFile ->
                // the false means to not include this file if it has no parents

                val nameMatches = remoteFile.name == localFile!!.name
                val parentMatches =
                    remoteFile.parents?.contains(fileSyncHelper!!.backupFolderMetadata.id) ?: false

                nameMatches && parentMatches
            }


            val debugStr = StringBuilder().run {
                append("\nfilename: ${localFile!!.name}")
                append("\nremote exists: ${remoteFileMetadata != null}")
                toString()
            }

            Log.d(tag, debugStr)

            fileSyncHelper!!.syncLocalAndRemote(localFile, remoteFileMetadata)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return BackupBinder()
    }

    inner class BackupBinder : Binder() {
        fun getService() : BackupService = this@BackupService
    }

}
