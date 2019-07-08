package com.sjfalken.wellnesstracker


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MediaListAdapter : RecyclerView.Adapter<MediaListAdapter.MediaListViewHolder>() {
    class MediaListViewHolder(private val cardView : CardView) : RecyclerView.ViewHolder(cardView)

    private val mediaFiles = mapOf(R.raw.meditation_affectionate_breathing to "Affectionate Breathing")

    override fun getItemCount(): Int = mediaFiles.size

    override fun onBindViewHolder(holder: MediaListViewHolder, position: Int) {
        holder.run {
            val cardContents = TextView(itemView.context).apply {
                val id = mediaFiles.keys.toList()[position]
                text = mediaFiles[id]
            }
            (itemView as CardView).addView(cardContents)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaListViewHolder {
        return MediaListViewHolder(CardView(parent.context))
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


