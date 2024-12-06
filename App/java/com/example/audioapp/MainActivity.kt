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
import com.example.audioapp.model.Songs
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.car.media.CarAudioManager
import android.widget.ImageView


import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.mpatric.mp3agic.Mp3File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.content.Intent

import VolumeDialogFragment


 ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////   
class MainActivity : AppCompatActivity(), SongAdapter.OnSongClickListener {
    //private var seekBar: SeekBar? = null
    private var songTime: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var btnPlayPause: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var btnPrev: ImageButton? = null
    private var songimg: ImageView? = null
    private var songTitle: TextView? = null
    //private var albumCart: CardView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex = 0
    private var movedSongIndex = 0
    private var songs: List<Songs> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    //private var cardView: CardView? = null
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private var isMonitoring = true
    private var counter: Int? = null
    private var flag: Int? = null
    private var songAdapter: SongAdapter? = null

    // Property IDs
    private val volumeUpPropertyId = 591397127
    private val volumeDownPropertyId = 591397128
    private val rightPropertyId = 591397129
    private val leftPropertyId = 591397136
    private val okPropertyId = 591397137
    private val rotaryPropertyId = 591397138
    private val joePropertyId = 591397145
    private lateinit var mCarAudioManager: CarAudioManager

 ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////   
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeCarProperties()

        val dialog = VolumeDialogFragment()

        val propertyValue: CarPropertyValue<Int>? = carPropertyManager?.getProperty(
    591397126, 0 // Replace with appropriate IDs
        )
    counter = propertyValue?.value ?: 0
    flag = counter
    
        // Set up folder path and retrieve songs
        //val folderPath = "/system/product/media/audio/Songs"
        val folderPath = "/system/product/media/audio/songs"
        songs = getSongsFromFolder(folderPath)

        songs.forEach { song ->
            val albumArtFile = ensureAlbumArtFile(song, cacheDir)
            song.albumArtPath = albumArtFile?.absolutePath
        }

        // UI components
        //seekBar = findViewById(R.id.progressBar)
        songTime = findViewById(R.id.songDuration)
        recyclerView = findViewById(R.id.songList)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        songTitle = findViewById(R.id.songTitle)
        songimg = findViewById(R.id.albumArt)
        

        // Set up RecyclerView
        recyclerView?.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(songs, this, this)
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

        /*seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })*/
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
                    monitorProperty(volumeUpPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(1) } // Volume Up
                    }
                    monitorProperty(volumeDownPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(0) } // Volume Down
                    }
                    monitorProperty(rightPropertyId) { value ->
                        if (value == 1) runOnUiThread { goToNextSong()} // Next Song
                    }
                    //goToNextSong()
                    monitorProperty(leftPropertyId) { value ->
                        if (value == 1) runOnUiThread { goToPreviousSong() } // Previous Song
                    }
                   monitorProperty(okPropertyId) { value ->
                        if (value == 1) runOnUiThread { togglePlayPause() } // Play/Pause
                    }
                    monitorProperty(rotaryPropertyId) { value ->
                        //value?.let { runOnUiThread { adjustVolume(it) } } // Rotary Input
                        if (value == 1) runOnUiThread { modeSheetOn()}
                    }
                    monitorProperty(591397126) { value ->
                        if((value ?: 0) != 0 && (counter ?: 0) == 0) {
                            counter = flag
                        }
                         if((value ?: 0) == 5005){
                            if (movedSongIndex == currentSongIndex) {
                                runOnUiThread { togglePlayPause() }
                            }
                            else {
                                currentSongIndex = movedSongIndex
                                //runOnUiThread { playSong(songs[currentSongIndex]) }
                                runOnUiThread { onSongClick(currentSongIndex) }
                            }
                            flag = counter
                            counter = 0
                        }
                        else if((value ?: 0) > (counter ?: 0)) {
                            /*while(counter != value) {
                                
                                counter++
                            }*/
                            counter = value
                            flag = counter
                            runOnUiThread { moveToNextSong()} 
                            
                        }
                        else if((value ?: 0) < (counter ?: 0)){
                            /*while(counter != value) {
                                goToPreviousSong()
                                counter--
                            }*/
                            counter = value
                            flag = counter
                            runOnUiThread { moveToPreviousSong()} 
                        }
                         
                        //value?.let { runOnUiThread { performCustomAction(it) } } // Custom Action
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
            btnPlayPause?.setImageResource(R.drawable.play)
        } else {
            mediaPlayer?.start()
            btnPlayPause?.setImageResource(R.drawable.pause_button)
            updateSeekBar()
        }
    }

    private fun performCustomAction(value: Int) {
        songTime?.text = value.toString()
    }

    private fun goToNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songs.size
        playSong(songs[currentSongIndex])
        songAdapter?.setSelectedIndex(currentSongIndex)
        //btnPlayPause?.setImageResource(R.drawable.pause_button)
    }


    private fun goToPreviousSong() {
        currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
        playSong(songs[currentSongIndex])
        songAdapter?.setSelectedIndex(currentSongIndex)
        //btnPlayPause?.setImageResource(R.drawable.pause_button)
    }

    private fun moveToNextSong() {
        //currentSongIndex = (currentSongIndex + 1) % songs.size
        movedSongIndex = (movedSongIndex + 1) % songs.size
        songAdapter?.setSelectedIndex(movedSongIndex)
        recyclerView?.scrollToPosition(movedSongIndex)
    }


    private fun moveToPreviousSong() {
        //currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
        movedSongIndex = if (movedSongIndex == 0) songs.size - 1 else movedSongIndex - 1
        songAdapter?.setSelectedIndex(movedSongIndex)
        recyclerView?.scrollToPosition(movedSongIndex)
    }

    private fun getSongsFromFolder(folderPath: String): List<Songs> {
        val folder = File(folderPath)
        if (folder.exists()) {
            val songFiles = folder.listFiles { file -> file.extension == "mp3" }
            return songFiles?.map { file ->
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                retriever.release()

                // Format duration as minutes:seconds
                val formattedDuration = durationMs?.let {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(it)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(it) % 60
                    String.format("%d:%02d", minutes, seconds)
                } ?: "0:00"

                Songs(file.nameWithoutExtension, file.absolutePath, formattedDuration)
            } ?: emptyList()
        } else {
            Toast.makeText(this, "Directory not found: $folderPath", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
    }



    private fun playSong(song: Songs) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }

        recyclerView?.scrollToPosition(currentSongIndex)
        songTitle?.text = song.title
        Glide.with(this)
            .load(song?.albumArtPath ?: R.drawable.download)  // Load file or use placeholder
            .into(songimg)
        btnPlayPause?.setImageResource(R.drawable.pause_button)
        //seekBar?.max = mediaPlayer?.duration ?: 0
        updateSeekBar()
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            //seekBar?.progress = it.currentPosition
            //  songTime?.text = formatTime(it.currentPosition) + " / " + formatTime(it.duration)
            songTime?.text = formatTime(it.currentPosition)
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
        //currentSongIndex = position
        //playSong(songs[currentSongIndex])
         mediaPlayer?.pause()
        val intent = Intent(this, Song::class.java)
        intent.putExtra("songs", ArrayList(songs))
        intent.putExtra("currentSongIndex", position)
        startActivity(intent)
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
            val dialog = VolumeDialogFragment()
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
                dialog.show(supportFragmentManager, "volumeDialog")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error adjusting volume with CarAudioManager", e)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Error adjusting volume", Toast.LENGTH_SHORT).show()
            }
        }
    }.start()

  
    
}
  private fun modeSheetOn(){
        val bottomSheet = IconDialogFragment()
        bottomSheet.show(supportFragmentManager, "iconBottomSheet")
    }

    fun ensureAlbumArtFile(song: Songs, cacheDir: File): File? {
        val albumArtFile = getAlbumArtFile(song, cacheDir)

        // If file exists, return it
        if (albumArtFile.exists()) {
            return albumArtFile
        }

        // Otherwise, extract and save album art using MediaMetadataRetriever
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(song.path)
            val albumArt = retriever.embeddedPicture
            retriever.release()

            if (albumArt != null) {
                // Decode and save album art
                val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
                FileOutputStream(albumArtFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                return albumArtFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null  // Return null if album art cannot be extracted
    }

    fun getAlbumArtFile(song: Songs, cacheDir: File): File {
        return File(cacheDir, "${song.title.hashCode()}.jpg")
    }
}
