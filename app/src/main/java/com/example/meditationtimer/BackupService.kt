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

    private val remoteFileMetadata : File = listRemoteFiles().find {remoteFile ->
        val nameMatches = remoteFile.name == FILENAME
        val parentMatches = remoteFile.parents?.contains(backupFolderMetadata.id) ?: false

        nameMatches && parentMatches
    } ?: run {
        val newFileMetadata = File().apply {
            name = FILENAME
            parents = listOf(backupFolderMetadata.id)
        }

        val content = localFile.readBytes()
        val contentStream = ByteArrayContent(null, content)
        googleDriveService.files().create(newFileMetadata, contentStream).apply { fields = "id" }.execute()
    }

    private val remoteFileContent = googleDriveService.files()
        .get(remoteFileMetadata.id).executeMediaAsInputStream().readBytes()

    private val localSyncDir = context.filesDir.parentFile.listFiles().find { file -> file.name == "databases" }!!
    private val localFile = java.io.File(
        "${context.filesDir.parent}/databases/${LogEntryDatabase.DB_NAME_PRIMARY}")

    private fun listRemoteFiles() : List<File> {
        return googleDriveService.files().list().apply{fields = "*"}
                .setSpaces(REMOTE_FS).execute().files
    }

    fun syncLocalAndRemote() {
        // wal checkpoint for the SQLite database
        // basically preparing the database to be synced
        LogEntryDatabase.checkpoint()
        val tag = "syncLocalAndRemote()"

        // if remote file does not exist, just copy this database over and return

        val restoreFilename = "$FILENAME.restore"
        java.io.File("${localSyncDir.path}/$restoreFilename").run {
            writeBytes(remoteFileContent)
            Log.d(tag, "file $name exists: ${exists()}")

            Log.d(tag, "file size: ${readBytes().size}")

//            LogEntryDatabase.insertEntriesFromDbFile(path)
        }

        val restoreDb = LogEntryDatabase.init(context, restoreFilename)
        val restoreEntries = restoreDb.entryDao().getAll()
        val localEntries = LogEntryDatabase.instance.entryDao().getAll()

        Log.d(tag, "Number of entries in restore db: ${restoreEntries.size}")

//            LogEntryDatabase.insertEntriesFromDbFile(path)

        // find the entries that are in the restore database but not local database
        for (restoreEntry in restoreEntries) {
            localEntries.find { localEntry -> localEntry.dateTime == restoreEntry.dateTime &&
                    localEntry.type == restoreEntry.type }.let { localEntry ->

                // if it doesn't exist locally, add it
                if (localEntry == null) {
                    LogEntryDatabase.instance.entryDao().insert(restoreEntry)
//                    localEntries.add(restoreEntry)
                }
            }
        }

        // need to checkpoint again before writing back new db file
        LogEntryDatabase.checkpoint()


        val content = localFile.readBytes()
        val contentStream = ByteArrayContent(null, content)
        googleDriveService.files().update(remoteFileMetadata.id, null, contentStream).execute()

        restoreDb.close()

        localSyncDir.list().forEach { Log.d(tag, it) }
    }

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

            localFile = java.io.File("${filesDir.parent}/databases/${LogEntryDatabase.DB_NAME_PRIMARY}")

            field = value
        }

    fun init(googleDriveService: Drive) : Task<BackupService> {
        return Tasks.call(executor, Callable {

            this.googleDriveService = googleDriveService

            this@BackupService
       })
    }

    fun syncDatabaseFiles() : Task<Unit> {
        return Tasks.call (executor, Callable {

            // will overwrite remote db with local
            fileSyncHelper!!.syncLocalAndRemote()
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
