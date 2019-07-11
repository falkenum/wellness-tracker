package com.sjfalken.wellnesstracker


import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MediaListAdapter(private val size : Int) : RecyclerView.Adapter<MediaListAdapter.MediaListViewHolder>() {
    class MediaListViewHolder(cardView : CardView) : RecyclerView.ViewHolder(cardView)
    class MediaListItemView(context: Context) : CardView(context) {
        private var mediaSelected = false
        var filename : String? = null
        init {
            background = ColorDrawable(context.getColor(R.color.colorAccent))
            background.alpha = 0
            setOnClickListener {
                mediaSelected = !mediaSelected

                val service = Intent(context, MediaService::class.java).apply {
                    putExtra(MediaService.ARG_FILENAME, filename!!)
                }
                if (mediaSelected) {
                    context.startService(service)
                }
                else {
                    context.stopService(service)
                }

                background.alpha = if (mediaSelected) 100 else 0
            }
        }
    }

//    private val mediaFiles = mapOf(R.raw.meditation_affectionate_breathing to "Affectionate Breathing")

    override fun getItemCount(): Int = size

    override fun onBindViewHolder(holder: MediaListViewHolder, position: Int) {
        holder.run {
            val filename = MediaFragment.neffMeditationNames.keys.toList()[position]
            val cardContents = TextView(itemView.context).apply {
                text = MediaFragment.neffMeditationNames[filename]
            }
            (itemView as CardView).addView(cardContents)
            (itemView as MediaListItemView).filename = filename
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaListViewHolder {
        return MediaListViewHolder(MediaListItemView(parent.context))
    }
}
class MediaFragment : BaseFragment(), MediaAccessor {
    companion object {
        const val neffMeditationsUrlPrefix = "https://self-compassion.org/wp-content/uploads/2016/11/"
        val neffMeditationNames = mapOf(
            "affectionatebreathing_cleaned.mp3" to "Affectionate breathing",
            "bodyscan_cleaned.mp3" to "Body scan",
            "LKM_cleaned.mp3" to "Loving kindness",
            "LKM.self-compassion_cleaned.mp3" to "Loving kindness and self-compassion",
            "noting.practice_cleaned.mp3" to "Noting emotions in the body"
        )
    }


    private fun retrieveMedia() {
        for (filename in neffMeditationNames.keys) {
            val path = neffMeditationsUrlPrefix + filename

            if (context!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                .listFiles { file -> file.name == filename }.isEmpty()) {

                val request = DownloadManager.Request(Uri.parse(path))
                    .setTitle(filename)
                    .setDestinationInExternalFilesDir(context!!, Environment.DIRECTORY_DOWNLOADS, filename)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

//            .setDestinationUri(Uri.fromFile(File(mediaDir.path + "/" + name)))

                val downloadManager = context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
            }

        }

//        Log.d("retrieveMedia()", context!!.getExternalFilesDir(null)!!.path)
//        val scheduler = Timer()
//        val task = object : TimerTask() {
//            override fun run() {
//                val queryResult = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
//                queryResult.moveToFirst()
//                val columnIndex = queryResult.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
//                val numBytes = queryResult.getInt(columnIndex)
//                Log.d("retrieveMedia()", numBytes.toString())
//            }
//
//        }
//        scheduler.scheduleAtFixedRate(task, 0, 1000)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retrieveMedia()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mainActivity = activity!! as MainActivity
        val rootView = RecyclerView(mainActivity).apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = MediaListAdapter(neffMeditationNames.size)
        }

        return rootView
    }
}


