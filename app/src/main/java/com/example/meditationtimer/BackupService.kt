package com.example.meditationtimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.meditationtimer.BackupService.Companion.REMOTE_FS
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class FileSyncHelper(private val googleDriveService : Drive, private val context: Context) {
    companion object {
        const val BACKUP_FOLDER_NAME = "WellnessTracker.backup"
        const val FILENAME = LogEntryDatabase.DB_NAME_PRIMARY
    }

    private val backupFolderMetadata : File = listRemoteFiles().find { it.name == BACKUP_FOLDER_NAME } ?: run {
            val newBackupFolderMetadata = File().apply {
                name = BACKUP_FOLDER_NAME
                val folderMimeType = "application/vnd.google-apps.folder"
                mimeType = folderMimeType
            }

            // create the new folder and store its new metadata from the server
            googleDriveService.files().create(newBackupFolderMetadata).apply { fields = "id" }.execute()
        }

    private val remoteFileMetadata : File? = listRemoteFiles().find {remoteFile ->
        val nameMatches = remoteFile.name == FILENAME
        val parentMatches = remoteFile.parents?.contains(backupFolderMetadata.id) ?: false

        nameMatches && parentMatches
    }

    private val localSyncDir = context.filesDir.parentFile.listFiles().find { file -> file.name == "databases" }!!

    private fun listRemoteFiles() : List<File> {
        return googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(REMOTE_FS).execute().files
    }

    private fun updateRemoteWithLocal(localFile : java.io.File) {
        val remoteExists = remoteFileMetadata != null

//            Log.d(tag, "file size: ${localFile.readBytes().size}")
        val content = localFile.readBytes()
        val contentStream = ByteArrayContent(null, content)

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

    private fun updateLocalWithRemote() {
        val bytes = googleDriveService.files()
            .get(remoteFileMetadata!!.id).executeMediaAsInputStream().readBytes()
        val tag = "updateLocalWithremote()"

        val restoreFilename = "restore-$FILENAME"

        java.io.File("${localSyncDir.path}/$restoreFilename").run {
            writeBytes(bytes)
            Log.d(tag, "file $name exists: ${exists()}")

            Log.d(tag, "file size: ${readBytes().size}")


//            LogEntryDatabase.insertEntriesFromDbFile(path)

            val restoreDb = LogEntryDatabase.init(context, restoreFilename)

            val restoreEntries = restoreDb.entryDao().getAll()
            val localEntries = LogEntryDatabase.instance.entryDao().getAll()

            Log.d(tag, "Number of entries in restore db: ${restoreEntries.size}")

//            LogEntryDatabase.insertEntriesFromDbFile(path)

            // find the entries that are in the restore database but not local database
            for (restoreEntry in restoreEntries) {
                localEntries.find { localEntry -> localEntry.dateTime == restoreEntry.dateTime &&
                                localEntry.type == restoreEntry.type }.let { entry ->

                    // if it doesn't exist locally, add it
                    if (entry == null) {
                        LogEntryDatabase.instance.entryDao().insert(restoreEntry)
                    }

                }
            }

            restoreDb.close()
        }

        localSyncDir.list().forEach { Log.d(tag, it) }
    }

    fun syncLocalAndRemote(localFile : java.io.File?) {

        val tag = "syncLocalAndRemote()"

        when (Pair(localFile == null, remoteFileMetadata == null)) {

            // update older with newer
            Pair(false, false) -> {
                val localLastModified = localFile!!.lastModified()
                val remoteLastModified = remoteFileMetadata!!.modifiedTime.value

                // if local file is newer
                if (localLastModified >= remoteLastModified) {
                    Log.d(tag, "local is newer; updating remote with local data")
                    updateRemoteWithLocal(localFile)
                }
                else {
                    Log.d(tag, "remote is newer; updating local with remote data")
                    updateLocalWithRemote()
                }
            }

            // copy local to remote
            Pair(false, true) -> updateRemoteWithLocal(localFile!!)
            // copy remote to local
            Pair(true, false) -> updateLocalWithRemote()
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
            localFile = java.io.File("${filesDir.parent}/databases/${LogEntryDatabase.DB_NAME_PRIMARY}")

            this.googleDriveService = googleDriveService

            this@BackupService
       })
    }

    fun backupDatabaseFiles() : Task<Unit> {
        return Tasks.call (executor, Callable {
            // wal checkpoint for the SQLite database
            // basically preparing the database to be synced
            LogEntryDatabase.checkpoint()

            // will overwrite remote db with local
            fileSyncHelper!!.syncLocalAndRemote(localFile)
        })
    }

    fun restoreDatabaseFiles() : Task<Unit> {
        return Tasks.call (executor, Callable {

//             will overwrite local db with remote
            fileSyncHelper!!.run {
                syncLocalAndRemote(null)
            }
        })
    }

//    fun syncDatabaseFiles() : Task<Unit> {
//        return Tasks.call (executor, Callable {
//
//            val tag = "syncDataBaseFile()"
//
//            Log.d(tag, "preparing to sync...")
//
//            val remoteFiles = fileSyncHelper!!.listRemoteFiles()
//
//            Log.d(tag, "Number of remote files: ${remoteFiles.size}")
//
//
//            val debugStr = StringBuilder().run {
//                append("\nfilename: ${localFile!!.name}")
//                append("\nremote exists: ${remoteFileMetadata != null}")
//                toString()
//            }
//
//            Log.d(tag, debugStr)
//
//            fileSyncHelper!!.syncLocalAndRemote(localFile, remoteFileMetadata)
//        })
//    }

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
