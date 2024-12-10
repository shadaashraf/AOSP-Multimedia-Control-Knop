package com.example.audioapp

import android.Manifest
import android.content.pm.PackageManager
import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
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
import android.car.media.CarAudioManager
import com.bumptech.glide.Glide
import com.mpatric.mp3agic.Mp3File
import android.graphics.Bitmap
import android.widget.ImageView
import android.util.Log
import java.util.concurrent.Executors
import android.content.Intent

import VolumeDialogFragment


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
    private var btnBack: ImageButton? = null
    private var songimg: ImageView? = null
    private var shuffleBtn:ImageButton?=null
    //private var cardView: CardView? = null
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private var isMonitoring = true
    private var counter: Int? = null
    private var flag: Int? = null
    private var isPlaying = true

    // Property IDs
    private val volumeUpPropertyId = 591397127
    private val volumeDownPropertyId = 591397128
    private val rightPropertyId = 591397129
    private val leftPropertyId = 591397136
    private val okPropertyId = 591397137
    private val rotaryPropertyId = 591397138
    private val joePropertyId = 591397145
    private lateinit var mCarAudioManager: CarAudioManager
     companion object {
        var sliderDialog = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        // Request runtime permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        car = Car.createCar(this)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            mCarAudioManager = car?.getCarManager(Car.AUDIO_SERVICE) as CarAudioManager

        val dialog = VolumeDialogFragment()

        val propertyValue: CarPropertyValue<Int>? = carPropertyManager?.getProperty(
    591397126, 0 // Replace with appropriate IDs
        )
    counter = propertyValue?.value ?: 0
    flag = counter

        // Set up folder path and retrieve songs
        songs = intent.getSerializableExtra("songs") as ArrayList<Songs>
        currentSongIndex = intent.getIntExtra("currentSongIndex", 0)

        // UI components
        seekBar = findViewById(R.id.seek_bar)
        songTime = findViewById(R.id.song_duration)
        songTitle = findViewById(R.id.song_title)
        btnPlayPause = findViewById<ImageButton>(R.id.btn_play_pause)
        val btnNext = findViewById<ImageButton>(R.id.btn_next)
        val btnPrev = findViewById<ImageButton>(R.id.btn_prev)
        mediaPlayerSection = findViewById(R.id.media_player_section)
        songListSection = findViewById(R.id.song_list_section)
        toggleButton = findViewById(R.id.btnPlayPause)
        closeButton = findViewById(R.id.close_button)
        songimg = findViewById(R.id.song_image)
        btnBack = findViewById(R.id.btn_back)
        shuffleBtn=findViewById(R.id.btn_shuffle)
        toggleButton?.setOnClickListener { toggleSongList() }
        closeButton?.setOnClickListener { closeSongList() }

        // Initially, show only the media player
        showMediaPlayerOnly()

        if (songs.isNotEmpty()) {
            // Play the first song on startup
            playSong(songs[currentSongIndex])
        } else {
        }

        /*btnPlayPause?.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause?.setImageResource(R.drawable.play)
            } else {
                mediaPlayer?.start()
                btnPlayPause?.setImageResource(R.drawable.pause_button)
                updateSeekBar()
            }
        }*/

        btnPlayPause?.setOnClickListener {
            togglePlayPause()
        }

        btnNext?.setOnClickListener {
            goToNextSong()
        }

        btnPrev?.setOnClickListener {
            goToPreviousSong()
        }

        // In Song Activity:
        btnBack?.setOnClickListener {
            backToHome()
        }

        shuffleBtn?.setOnClickListener {
            val randomSong = (0 until songs.size-1).random()
            currentSongIndex=randomSong
             playSong(songs[currentSongIndex])

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
            setOnCompletionListener {
                goToNextSong()
            }
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
            if(it.duration - it.currentPosition <= 1000) {
                goToNextSong()
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%d:%02d", minutes, seconds)
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
            }
        }
    }.start()

  
    
}
  private fun modeSheetOn(){
         val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag("ImageSliderDialogFragment")

        if (existingFragment != null && existingFragment.isVisible) {
            // If the fragment is visible, dismiss it
            (existingFragment as? ImageSliderDialogFragment)?.dismiss()
        } else {
            // If the fragment is not visible, show it
            val sliderItems = listOf(
                SliderItem(R.drawable.music, "com.example.audioapp"),
                SliderItem(R.drawable.settings, "com.android.car.settings"),
                SliderItem(R.drawable.radio, "com.android.car.radio"),
                SliderItem(R.drawable.sms, "com.android.car.messenger"),
                SliderItem(R.drawable.app, "com.android.car.dialer")
            )

            val dialogFragment = ImageSliderDialogFragment.newInstance(sliderItems)
            dialogFragment.show(fragmentManager, "ImageSliderDialogFragment")
        }
    }

    fun getAlbumArtFile(song: Songs, cacheDir: File): File {
        return File(cacheDir, "${song.title.hashCode()}.jpg")
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        executorService.shutdown()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        car?.disconnect()
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

    override fun onResume() {
        super.onResume()
        // Resume monitoring thread when the activity comes back to the foreground
        isMonitoring = true
        initializeCarProperties()
    }

    override fun onPause() {
        super.onPause()
        // Stop the monitoring thread when the activity is paused
        isMonitoring = false
    }

    override fun onStop() {
        super.onStop()
        // Release resources when the activity is stopped
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun initializeCarProperties() {
        try {
            executorService.execute {
                while (isMonitoring) {
                    monitorProperty(volumeUpPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(1) } // Volume Up
                    }
                    monitorProperty(volumeDownPropertyId) { value ->
                        if (value == 1) runOnUiThread { adjustVolume(0) } // Volume Down
                    }
                    monitorProperty(rightPropertyId) { value ->
                        if (value == 1) {
                             if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToNextSong()}
                            } 
                            else {
                                runOnUiThread { goToNextSong()} // Next Song
                            }
                        } // Next Song
                    }
                    //goToNextSong()
                    monitorProperty(leftPropertyId) { value ->
                        if (value == 1){
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToPreviousSong()}
                            } 
                            else {
                                runOnUiThread { goToPreviousSong() } // Previous Song
                            }
                        } // Previous Song
                    }
                   monitorProperty(okPropertyId) { value ->
                        if (value == 1) {
                            if(sliderDialog) {
                                runOnUiThread {(supportFragmentManager.findFragmentByTag("ImageSliderDialogFragment") as? ImageSliderDialogFragment)
                                    ?.onAppClick(ImageSliderDialogFragment.currentSongIndex)}
                            }
                            else {
                                runOnUiThread { togglePlayPause() } // Play/Pause
                            }
                        }// Play/Pause
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
                            
                            runOnUiThread { backToHome() }
                            flag = counter
                            counter = 0
                        }
                        /*else if((value ?: 0) > (counter ?: 0)) {
                            
                            counter = value
                            flag = counter
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToNextSong()}
                            } 
                            else {
                                runOnUiThread { moveToNextSong()} 
                            }
                            
                        }
                        else if((value ?: 0) < (counter ?: 0)){
                            
                            counter = value
                            flag = counter
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToPreviousSong()}
                            } 
                            else {
                                runOnUiThread { moveToPreviousSong()} 
                            }
                        }*/
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
            isPlaying = false
        } else {
            mediaPlayer?.start()
            btnPlayPause?.setImageResource(R.drawable.pause_button)
            updateSeekBar()
            isPlaying = true
        }
    }

    private fun performCustomAction(value: Int) {
        songTime?.text = value.toString()
    }

    private fun goToNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songs.size
        playSong(songs[currentSongIndex])
    }


    private fun goToPreviousSong() {
        currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
        playSong(songs[currentSongIndex])
    }

    private fun backToHome() {
        // Check if the song was playing and if so, continue playback from the current index
            //val isPlaying = intent.getBooleanExtra("isPlaying", true)
            //val currentIndex = intent.getIntExtra("currentSongIndex", 0)
            val currentPosition = mediaPlayer?.currentPosition ?: 0

            // Create an Intent to go back to MainActivity
            val backIntent = Intent(this, MainActivity::class.java)
            backIntent.putExtra("currentSongIndex", currentSongIndex)
            backIntent.putExtra("isPlaying", isPlaying) // Pass the playback state
            backIntent.putExtra("currentPosition", currentPosition)
            
            // Start MainActivity
            startActivity(backIntent)

            // Optionally, finish current activity if you don't want it to be in the back stack
            finish()
    }
}
