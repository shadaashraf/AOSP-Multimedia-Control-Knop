package com.example.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.audioapp.model.Songs
import com.bumptech.glide.Glide
import android.content.Context
import android.widget.ImageView
import android.graphics.Bitmap
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import android.graphics.Color

class SongAdapter(
    private val songs: List<Songs>,
    private val listener: OnSongClickListener,
    val context: Context,
    private var selectedIndex: Int = 0
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    interface OnSongClickListener {
        fun onSongClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
    
        holder.songTitle?.text = song.title
        holder.songDuration?.text = song.duration
        holder.itemView.setOnClickListener { listener.onSongClick(position) }
        Glide.with(context)
            .load(song.albumArtPath ?: R.drawable.download)  // Load file or use placeholder
            .into(holder.albumArt)
        
        if (position == selectedIndex) {
            holder.albumCart?.setCardBackgroundColor(Color.parseColor("#A9A9A9"))
        } else {
            holder.albumCart?.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }
    }

    fun setSelectedIndex(index: Int) {
        if(index != selectedIndex) {
            val prevIndex = selectedIndex
            selectedIndex = index
            if(prevIndex != -1) {
                notifyItemChanged(prevIndex)
            }
            notifyItemChanged(selectedIndex)
        }
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songTitle: TextView? = itemView.findViewById(R.id.songTitle)
        val songDuration: TextView? = itemView.findViewById(R.id.songDuration)
        val albumArt: ImageView? = itemView.findViewById(R.id.albumArt)
        val albumCart: CardView? = itemView.findViewById(R.id.albumCard)
    }
}
