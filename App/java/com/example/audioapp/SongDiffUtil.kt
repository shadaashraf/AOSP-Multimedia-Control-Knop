package com.example.audioapp

import com.example.audioapp.model.Songs
import androidx.recyclerview.widget.DiffUtil

class SongDiffUtil : DiffUtil.ItemCallback<Songs>() {
    override fun areItemsTheSame(oldItem: Songs, newItem: Songs): Boolean {
        // Compare unique identifiers if available; otherwise compare objects
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Songs, newItem: Songs): Boolean {
        return oldItem == newItem
    }
}
