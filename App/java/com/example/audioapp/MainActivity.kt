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
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat


import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.mpatric.mp3agic.Mp3File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.content.Intent

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder


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
    companion object {
        var sliderDialog = false
    }
    

    // Property IDs
    private val volumeUpPropertyId = 591397127
    private val volumeDownPropertyId = 591397128
    private val rightPropertyId = 591397129
    private val leftPropertyId = 591397136
    private val okPropertyId = 591397137
    private val rotaryPropertyId = 591397138
    private val joePropertyId = 591397145
    private lateinit var mCarAudioManager: CarAudioManager

    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBound = true
            // Your service connection logic here
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            // Handle disconnection logic
        }
    }

 ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////   
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        //val folderPath = "/system/product/media/audio/Songs"
        songs = getSongs()

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

        songTitle?.text = songs[0].title
        Glide.with(this)
            .load(songs[0]?.albumArtPath ?: R.drawable.download)  // Load file or use placeholder
            .into(songimg)

        if (songs.isNotEmpty()) {
            //playSong(songs[currentSongIndex]) // Play the first song on startup
            val currentIndex = intent.getIntExtra("currentSongIndex", -1)
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            val currentPosition = intent.getIntExtra("currentPosition", 0)

            if (currentIndex != -1 && isPlaying) {
                // Resume the song from the current index
                currentSongIndex = currentIndex
                playSong(songs[currentIndex])
                 mediaPlayer?.seekTo(currentPosition) // Seek to the position if needed
                //mediaPlayer?.start() // Start playing
                //playSong(songs[currentIndex])
            }
            else if (currentIndex != -1 && !isPlaying) {
                // Resume the song from the current index
                currentSongIndex = currentIndex
                playSong(songs[currentIndex])
                togglePlayPause()
                 mediaPlayer?.seekTo(currentPosition) // Seek to the position if needed
                //mediaPlayer?.start() // Start playing
                //playSong(songs[currentIndex])
            }
            
        } else {
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
 private fun getSongs(): List<Songs> {
    val usbMountPoint = "/mnt/media_rw/"
    val defaultFolderPath = "/system/product/media/audio/songs"
    val usbFiles: Array<File>?
    var folderPath: String = defaultFolderPath // Declare folderPath here once

    // Check if USB directory exists
    val usbDir = File(usbMountPoint)
    if (usbDir.exists() && usbDir.isDirectory) {
        usbFiles = usbDir.listFiles()

        if (usbFiles != null && usbFiles.isNotEmpty()) {
            // Log the files to see what's in the directory
            usbFiles.forEach { file ->
                Log.d("USB_Mount", "File found: ${file.absolutePath}")
            }
            val usbFolder = usbFiles.firstOrNull { it.isDirectory }
            folderPath = usbFolder?.absolutePath ?: defaultFolderPath
        } else {
            // If no files are found in the USB directory, fallback to default folder
        }
    } else {
        // If USB directory does not exist (i.e., it was unmounted), fallback to default folder
        usbFiles = null
    }

    return getSongsFromFolder(folderPath)
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
                        }
                        
                    }
                    //goToNextSong()
                    monitorProperty(leftPropertyId) { value ->
                        if (value == 1) {
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToPreviousSong()}
                            } 
                            else {
                                runOnUiThread { goToPreviousSong() } // Previous Song
                            }
                        }
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
                        }
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
                            if(sliderDialog) {
                                (supportFragmentManager.findFragmentByTag("ImageSliderDialogFragment") as? ImageSliderDialogFragment)
                                    ?.onAppClick(ImageSliderDialogFragment.currentSongIndex)
                            }
                            else {
                                if (movedSongIndex == currentSongIndex) {
                                    runOnUiThread { togglePlayPause() }
                                }
                                else {
                                    currentSongIndex = movedSongIndex
                                    //runOnUiThread { playSong(songs[currentSongIndex]) }
                                    runOnUiThread { onSongClick(currentSongIndex) }
                                }
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
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToNextSong()}
                            } 
                            else {
                                runOnUiThread { moveToNextSong()} 
                            }
                            
                        }
                        else if((value ?: 0) < (counter ?: 0)){
                            /*while(counter != value) {
                                goToPreviousSong()
                                counter--
                            }*/
                            counter = value
                            flag = counter
                            if(sliderDialog) {
                                runOnUiThread { ImageSliderDialogFragment.goToPreviousSong()}
                            } 
                            else {
                                runOnUiThread { moveToPreviousSong()} 
                            }
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
            setOnCompletionListener {
                goToNextSong()
            }
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

    override fun onSongClick(position: Int) {
        //currentSongIndex = position
        //playSong(songs[currentSongIndex])
         mediaPlayer?.pause()
        val intent = Intent(this, Song::class.java)
        intent.putExtra("songs", ArrayList(songs))
        intent.putExtra("currentSongIndex", position)
        startActivity(intent)
    }

    /*override fun onResume() {
        super.onResume()
        // Resume monitoring thread when the activity comes back to the foreground
        isMonitoring = true
        initializeCarProperties()
    }*/

    override fun onResume() {
        super.onResume()
        isMonitoring = true
        initializeCarProperties()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        val intent = Intent(this, SliderService::class.java)
        stopService(intent)
    }


    override fun onPause() {
        super.onPause()
        // Stop the monitoring thread when the activity is paused
        val intent = Intent(this, SliderService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isMonitoring = false
    }

    override fun onStop() {
        super.onStop()
        // Release resources when the activity is stopped
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        
        isMonitoring = false
        executorService.shutdown()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        car?.disconnect()
         if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
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
  /*private fun modeSheetOn(){
        val bottomSheet = IconDialogFragment()
        bottomSheet.show(supportFragmentManager, "iconBottomSheet")
    }*/

    private fun modeSheetOn(){
        /*val sliderItems = listOf(
            SliderItem(R.drawable.download3, "com.example.audioapp"),
            SliderItem(R.drawable.images1, "com.android.car.settings"),
            SliderItem(R.drawable.images2, "com.android.car.radio"),
            SliderItem(R.drawable.download2,"com.android.car.messenger"),
            SliderItem(R.drawable.download1,"com.android.car.dialer")
        )
        
        val dialogFragment = ImageSliderDialogFragment.newInstance(sliderItems)
        dialogFragment.show(supportFragmentManager, "ImageSliderDialogFragment")*/
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

            /*
            val sliderItems = listOf(
                SliderItem(R.drawable.ic_launcher_background, "com.example.audioapp"),
                SliderItem(R.drawable.settings, "com.android.car.settings"),
                SliderItem(R.drawable.radio, "com.android.car.radio"),
                SliderItem(R.drawable.sms, "com.android.car.messenger"),
                SliderItem(R.drawable.dialer, "com.android.car.dialer")
            ) */

            val dialogFragment = ImageSliderDialogFragment.newInstance(sliderItems)
            dialogFragment.show(fragmentManager, "ImageSliderDialogFragment")
        }
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
