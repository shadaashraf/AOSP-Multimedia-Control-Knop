package com.example.audioapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audioapp.model.Songs
import java.io.File
import java.util.concurrent.TimeUnit
import com.bumptech.glide.Glide
import com.mpatric.mp3agic.Mp3File
import android.graphics.Bitmap
import android.widget.ImageView


class Song : AppCompatActivity() {
    private var seekBar: SeekBar? = null
    private var songTime: TextView? = null
    private var songTitle: TextView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex = 0
    private var songs: List<Songs> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var isSongListVisible = false

    private var mediaPlayerSection: FrameLayout? = null
    private var songListSection: FrameLayout? = null
    private var toggleButton: ImageButton? = null
    private var closeButton: ImageButton? = null
    private var btnPlayPause: ImageButton? = null
    private var songimg: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        // Request runtime permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        // Set up folder path and retrieve songs
        songs = intent.getSerializableExtra("songs") as ArrayList<Songs>
        currentSongIndex = intent.getIntExtra("currentSongIndex", 0)

        // UI components
        seekBar = findViewById(R.id.seek_bar)
        songTime = findViewById(R.id.song_duration)
        songTitle = findViewById(R.id.song_title)
        btnPlayPause = findViewById<ImageButton>(R.id.btn_play_pause)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btn_prev)
        mediaPlayerSection = findViewById(R.id.media_player_section)
        songListSection = findViewById(R.id.song_list_section)
        toggleButton = findViewById(R.id.btnPlayPause)
        closeButton = findViewById(R.id.close_button)
        songimg = findViewById(R.id.song_image)

        toggleButton?.setOnClickListener { toggleSongList() }
        closeButton?.setOnClickListener { closeSongList() }

        // Initially, show only the media player
        showMediaPlayerOnly()

        if (songs.isNotEmpty()) {
            // Play the first song on startup
            playSong(songs[currentSongIndex])
        } else {
            Toast.makeText(this, "No songs found in the folder", Toast.LENGTH_SHORT).show()
        }

        btnPlayPause?.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause?.setImageResource(R.drawable.play)
            } else {
                mediaPlayer?.start()
                btnPlayPause?.setImageResource(R.drawable.pause_button)
                updateSeekBar()
            }
        }

        btnNext?.setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            playSong(songs[currentSongIndex])
            btnPlayPause?.setImageResource(R.drawable.pause_button)
        }

        btnPrev?.setOnClickListener {
            currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
            playSong(songs[currentSongIndex])
            btnPlayPause?.setImageResource(R.drawable.pause_button)
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showMediaPlayerOnly() {
        // Cast to FrameLayout.LayoutParams and update width/height
        val mediaParams = mediaPlayerSection?.layoutParams as? FrameLayout.LayoutParams
        mediaParams?.let {
            it.width = 2 // Full width for media player
            mediaParams.height = FrameLayout.LayoutParams.WRAP_CONTENT // Adjust as needed
            mediaPlayerSection?.layoutParams = it
        }

        val songListParams = songListSection?.layoutParams as? FrameLayout.LayoutParams
        songListParams?.let {
            it.width = 0 // Hide song list (width set to 0)
            songListParams.height = FrameLayout.LayoutParams.WRAP_CONTENT // Adjust as needed
            songListSection?.layoutParams = it
        }

        toggleButton?.visibility = View.VISIBLE // Show toggle button when media player is alone
    }

    private fun showSplitScreen() {
        // Adjust layout for split screen, where both sections share space
        val mediaParams = mediaPlayerSection?.layoutParams as? FrameLayout.LayoutParams
        mediaParams?.let {
            it.width = 1 // Half width
            mediaParams.height = FrameLayout.LayoutParams.MATCH_PARENT // Adjust as needed
            mediaPlayerSection?.layoutParams = it
        }

        val songListParams = songListSection?.layoutParams as? FrameLayout.LayoutParams
        songListParams?.let {
            it.width = 1 // Other half width
            songListParams.height = FrameLayout.LayoutParams.MATCH_PARENT // Adjust as needed
            songListSection?.layoutParams = it
        }

        toggleButton?.visibility = View.GONE // Hide toggle button when in split view
    }

    private fun toggleSongList() {
        if (isSongListVisible) {
            showMediaPlayerOnly()
        } else {
            showSplitScreen()
        }
        isSongListVisible = !isSongListVisible
    }

    private fun closeSongList() {
        // Hide the song list and show only the media player
        showMediaPlayerOnly()
        isSongListVisible = false // Ensure the state reflects that the song list is hidden
    }

    private fun playSong(song: Songs) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }

        // Update UI with the current song details
        songTitle?.text = song.title
        /*Glide.with(this)
            .load(song?.albumArtPath ?: R.drawable.download)  // Load file or use placeholder
            .into(songimg)*/
        songimg?.let {
        Glide.with(this)
            .load(song?.albumArtPath ?: R.drawable.download)
            .into(it)
        }
        btnPlayPause?.setImageResource(R.drawable.pause_button)
        seekBar?.max = mediaPlayer?.duration ?: 0
        updateSeekBar()
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            seekBar?.progress = it.currentPosition
            songTime?.text = formatTime(it.currentPosition) + " / " + formatTime(it.duration)
            if (it.isPlaying) {
                handler.postDelayed({ updateSeekBar() }, 1000)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun getSongsFromFolder(folderPath: String): List<Songs> {
        val folder = File(folderPath)
        if (folder.exists()) {
            val songFiles = folder.listFiles { file -> file.extension == "mp3" }
            return songFiles?.map { file -> Songs(file.nameWithoutExtension, file.absolutePath) } ?: emptyList()
        } else {
            Toast.makeText(this, "Directory not found: $folderPath", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
