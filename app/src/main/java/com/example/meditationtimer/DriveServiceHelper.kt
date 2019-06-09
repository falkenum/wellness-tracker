/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.meditationtimer

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.telecom.Call
import androidx.core.util.Pair
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
class DriveServiceHelper(private val mDriveService: Drive) {
    val executor = Executors.newSingleThreadExecutor()

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    fun createFile(): Task<String> {
        return Tasks.call (executor, Callable {
            val metadata = File()
                .setParents(listOf("root"))
                .setMimeType("text/plain")
                .setName("Untitled file")

            val googleFile = mDriveService.files().create(metadata).execute()
                ?: throw IOException("Null result when requesting file creation.")

            googleFile.id
        })
    }

    /**
     * Opens the file identified by `fileId` and returns a [Pair] of its name and
     * contents.
     */
    fun readFile(fileId: String): Task<Pair<String, String>> {
        return Tasks.call (executor, Callable {
            // Retrieve the metadata as a File object.
            val metadata = mDriveService.files().get(fileId).execute()
            val name = metadata.name

            // Stream the file contents to a String.
            mDriveService.files().get(fileId).executeMediaAsInputStream().use { `is` ->
                BufferedReader(InputStreamReader(`is`)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line : String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                    val contents = stringBuilder.toString()

                    Pair.create(name, contents)
                }
            }
        })
    }

    /**
     * Updates the file identified by `fileId` with the given `name` and `content`.
     */
    fun saveFile(fileId: String, name: String, content: String): Task<Any> {
        return Tasks.call (executor, Callable {
            // Create a File containing any metadata changes.
            val metadata = File().setName(name)

            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString("text/plain", content)

            // Update the metadata and contents.
            mDriveService.files().update(fileId, metadata, contentStream).execute()
            Any()
        })
    }

    /**
     * Returns a [FileList] containing all the visible files in the user's My Drive.
     *
     *
     * The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the [Google
 * Developer's Console](https://play.google.com/apps/publish) and be submitted to Google for verification.
     */
    fun queryFiles(): Task<FileList> {
        return Tasks.call (executor, Callable {
             mDriveService.files().list().setSpaces("drive").execute()
        })
    }

    /**
     * Returns an [Intent] for opening the Storage Access Framework file picker.
     */
    fun createFilePickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"

        return intent
    }

    /**
     * Opens the file at the `uri` returned by a Storage Access Framework [Intent]
     * created by [.createFilePickerIntent] using the given `contentResolver`.
     */
    fun openFileUsingStorageAccessFramework(
        contentResolver: ContentResolver, uri: Uri
    ): Task<Pair<String, String>> {
        return Tasks.call (executor, Callable {
            // Retrieve the document's display name from its metadata.
            lateinit var name: String
            contentResolver.query(uri, null, null, null, null)!!.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    name = cursor.getString(nameIndex)
                } else {
                    throw IOException("Empty cursor returned for file.")
                }
            }

            // Read the document's contents as a String.
            lateinit var content: String
            contentResolver.openInputStream(uri)!!.use { `is` ->
                BufferedReader(InputStreamReader(`is`)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line : String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                    content = stringBuilder.toString()
                }
            }

            Pair.create(name, content)
        })
    }
}
