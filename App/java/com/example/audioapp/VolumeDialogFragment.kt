import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.audioapp.R

class VolumeDialogFragment : DialogFragment() {

    private var volumeLevel = 50 // Initial volume level (50%)
    var onVolumeChanged: ((Int) -> Unit)? = null // Callback for volume change

    // Handler to manage dialog auto-dismiss
    private val handler = Handler(Looper.getMainLooper())
    private var isSeekBarChanged = false
    private var audioManager: AudioManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_volume_dialog, container, false)
    }

    override fun onStart() {
        super.onStart()

        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val volumeSlider = dialog?.findViewById<SeekBar>(R.id.volumeSlider)
        val volumeValueText = dialog?.findViewById<TextView>(R.id.volumeLevelLabel)

        // Get the maximum system volume
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100

        // Get the current system volume
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 50
        volumeLevel = (currentVolume * 100) / maxVolume // Map to 0-100 range

        // Set initial volume level display
        volumeValueText?.text = "$volumeLevel%"

        // Set the volume slider progress to the current system volume
        volumeSlider?.progress = volumeLevel

        // Update volume level when the slider changes
        volumeSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                isSeekBarChanged = true // Mark that a change has occurred
                volumeLevel = progress
                volumeValueText?.text = "$volumeLevel%" // Update the volume level text
                adjustSystemVolume(volumeLevel) // Adjust the system volume
                onVolumeChanged?.invoke(volumeLevel) // Notify volume level change
                resetDismissTimer() // Reset the timer to avoid auto-dismiss
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Stop the auto-dismiss timer while the user is interacting
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Restart the timer when the user stops interacting
                startDismissTimer()
            }
        })

        // Set dialog background to transparent (remove white background)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog dimensions
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT, // Width set to wrap content
            ViewGroup.LayoutParams.WRAP_CONTENT  // Height set to wrap content (or fixed value)
        )

        // Position the dialog at the right center of the screen
        val layoutParams = dialog?.window?.attributes
        layoutParams?.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL // Right-center position
        dialog?.window?.attributes = layoutParams

        // Start the auto-dismiss timer
        startDismissTimer()
    }

    private fun adjustSystemVolume(volume: Int) {
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
        val newVolume = (volume * maxVolume) / 100 // Map 0-100 range to system volume range
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    private fun startDismissTimer() {
        handler.postDelayed({
            if (!isSeekBarChanged) { // Close the dialog only if no changes have occurred
                dismiss()
            } else {
                // Reset the flag after dismiss logic is attempted
                isSeekBarChanged = false
            }
        }, 2000) // 2 seconds delay
    }

    private fun resetDismissTimer() {
        handler.removeCallbacksAndMessages(null) // Clear all pending callbacks
        startDismissTimer() // Restart the timer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Clean up the handler to prevent memory leaks
    }

    companion object {
        @JvmStatic
        fun newInstance() = VolumeDialogFragment()
    }
}
