package com.example.audioapp

import com.example.audioapp.model.Song
import androidx.recyclerview.widget.DiffUtil

class SongDiffUtil : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        // Compare unique identifiers if available; otherwise compare objects
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem == newItem
    }
}
