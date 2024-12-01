package com.example.audioapp

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audioapp.model.Song
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.car.media.CarAudioManager
class MainActivity : AppCompatActivity(), SongAdapter.OnSongClickListener {
    private var seekBar: SeekBar? = null
    private var songTime: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var btnPlayPause: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var btnPrev: ImageButton? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex = 0
    private var songs: List<Song> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private var isMonitoring = true

    // Property IDs
    private val volumeUpPropertyId = 591397127
    private val volumeDownPropertyId = 591397128
    private val rightPropertyId = 591397129
    private val leftPropertyId = 591397136
    private val okPropertyId = 591397137
    private val rotaryPropertyId = 591397138
    private val joePropertyId = 591397145
    private lateinit var mCarAudioManager: CarAudioManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeCarProperties()
      
        // Set up folder path and retrieve songs
        val folderPath = "/system/product/media/audio/Songs"
        songs = getSongsFromFolder(folderPath)

        // UI components
        seekBar = findViewById(R.id.progressBar)
        songTime = findViewById(R.id.songDuration)
        recyclerView = findViewById(R.id.songList)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        

        // Set up RecyclerView
        recyclerView?.layoutManager = LinearLayoutManager(this)
        val songAdapter = SongAdapter(songs, this)
        recyclerView?.adapter = songAdapter

        if (songs.isNotEmpty()) {
            playSong(songs[currentSongIndex]) // Play the first song on startup
        } else {
            Toast.makeText(this, "No songs found in the folder", Toast.LENGTH_SHORT).show()
        }

        btnPlayPause?.setOnClickListener {
            togglePlayPause()
        }

        btnNext?.setOnClickListener {
            goToNextSong()
        }

        btnPrev?.setOnClickListener {
            goToPreviousSong()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

private fun monitorProperty(propertyId: Int, action: (value: Int?) -> Unit) {
    try {
        // Retrieve the property value
        val carPropertyValue: CarPropertyValue<*> = carPropertyManager?.getProperty(
            Integer::class.java, propertyId, 0
        ) ?: throw IllegalArgumentException("Failed to retrieve property value")

        // Check and safely cast the value to Int
        val value = carPropertyValue.value?.let {
            // Ensure it is of type Int
            if (it is Int) it else null
        }

        Log.d("CAR", "Property $propertyId value: $value")
        action(value) // Call the action with the value
    } catch (e: Exception) {
        Log.e("CAR", "Error monitoring propertyyyyyyyyyyyyyyyy $propertyId", e)
    }
}

    private fun initializeCarProperties() {
        try {
            car = Car.createCar(this)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            mCarAudioManager = car?.getCarManager(Car.AUDIO_SERVICE) as CarAudioManager
            executorService.execute {
                while (isMonitoring) {
                   /* monitorProperty(volumeUpPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(1) } // Volume Up
                    }*/
                    monitorProperty(volumeDownPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(0) } // Volume Down
                    }
                    monitorProperty(rightPropertyId) { value ->
                        if (value == 1) runOnUiThread { goToNextSong() } // Next Song
                    }
                    monitorProperty(leftPropertyId) { value ->
                        if (value == 1) runOnUiThread { goToPreviousSong() } // Previous Song
                    }
                   /* monitorProperty(okPropertyId) { value ->
                        if (value == 1) runOnUiThread { togglePlayPause() } // Play/Pause
                    }*/
                    monitorProperty(rotaryPropertyId) { value ->
                        value?.let { runOnUiThread { adjustVolume(it) } } // Rotary Input
                    }
                    monitorProperty(joePropertyId) { value ->
                        value?.let { runOnUiThread { performCustomAction(it) } } // Custom Action
                    }
                    Thread.sleep(100) // Polling interval
                }
            }
        } catch (e: Exception) {
            Log.e("CAR", "Error initializing Car API", e)
        }
    }

  

    private fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            btnPlayPause?.setImageResource(R.drawable.play_buttton)
        } else {
            mediaPlayer?.start()
            btnPlayPause?.setImageResource(R.drawable.pause)
            updateSeekBar()
        }
    }

    private fun performCustomAction(value: Int) {
        //Toast.makeText(this, "Custom action triggered: $value", Toast.LENGTH_SHORT).show()
    }

    private fun goToNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songs.size
        playSong(songs[currentSongIndex])
        btnPlayPause?.setImageResource(R.drawable.pause)
    }

    private fun goToPreviousSong() {
        currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
        playSong(songs[currentSongIndex])
        btnPlayPause?.setImageResource(R.drawable.pause)
    }

    private fun getSongsFromFolder(folderPath: String): List<Song> {
        val folder = File(folderPath)
        if (folder.exists()) {
            val songFiles = folder.listFiles { file -> file.extension == "mp3" }
            return songFiles?.map { file -> Song(file.nameWithoutExtension, file.absolutePath) } ?: emptyList()
        } else {
            Toast.makeText(this, "Directory not found: $folderPath", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
    }

    private fun playSong(song: Song) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }

        recyclerView?.scrollToPosition(currentSongIndex)
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

    override fun onSongClick(position: Int) {
        currentSongIndex = position
        playSong(songs[currentSongIndex])
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        executorService.shutdown()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        car?.disconnect()
    }
   private fun adjustVolume(gpioState: Int) {
    Thread {
        try {
            val primaryZone = CarAudioManager.PRIMARY_AUDIO_ZONE
            val volumeGroupCount = mCarAudioManager.volumeGroupCount // Alternative to getVolumeGroupIdsForAudioZone
            if (volumeGroupCount > 0) {
                val volumeGroupId = 0 // Assuming the first group; adapt as needed
                val currentVolume = mCarAudioManager.getGroupVolume(volumeGroupId)
                val maxVolume = mCarAudioManager.getGroupMaxVolume(volumeGroupId)

                if (gpioState == 1 && currentVolume < maxVolume) {
                    mCarAudioManager.setGroupVolume(volumeGroupId, currentVolume + 1, 0) // Remove FLAG_SHOW_UI
                } else if (gpioState == 0 && currentVolume > 0) {
                    mCarAudioManager.setGroupVolume(volumeGroupId, currentVolume - 1, 0) // Remove FLAG_SHOW_UI
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error adjusting volume with CarAudioManager", e)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Error adjusting volume", Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}
}
