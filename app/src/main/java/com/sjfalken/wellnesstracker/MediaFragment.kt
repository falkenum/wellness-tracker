package com.sjfalken.wellnesstracker


import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MediaListAdapter : RecyclerView.Adapter<MediaListAdapter.MediaListViewHolder>() {
    class MediaListViewHolder(cardView : CardView) : RecyclerView.ViewHolder(cardView)
    class MediaListItemView(context: Context) : CardView(context) {
        private var mediaSelected = false
        var playbackId : Int? = null
        init {
            background = ColorDrawable(context.getColor(R.color.colorAccent))
            background.alpha = 0
            setOnClickListener {
                mediaSelected = !mediaSelected
                val service = Intent(context, MediaService::class.java).apply {
                    putExtra(MediaService.ARG_PLAYBACK_ID, playbackId!!)
                }

                context.startForegroundService(service)

                background.alpha = if (mediaSelected) 100 else 0
            }
        }
    }

    private val mediaFiles = mapOf(R.raw.meditation_affectionate_breathing to "Affectionate Breathing")

    override fun getItemCount(): Int = mediaFiles.size

    override fun onBindViewHolder(holder: MediaListViewHolder, position: Int) {
        holder.run {
            val playbackId = mediaFiles.keys.toList()[position]
            val cardContents = TextView(itemView.context).apply {
                text = mediaFiles[playbackId]
            }
            (itemView as CardView).addView(cardContents)
            (itemView as MediaListItemView).playbackId = playbackId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaListViewHolder {
        return MediaListViewHolder(MediaListItemView(parent.context))
    }
}
class MediaFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mainActivity = activity!! as MainActivity
        val rootView = RecyclerView(mainActivity).apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = MediaListAdapter()
        }

        return rootView
    }
}


