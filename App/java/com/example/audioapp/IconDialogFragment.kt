package com.example.audioapp

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment

class IconDialogFragment : DialogFragment() {

    private val autoDismissHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_icon_dialog, container, false)

        // Set up the image buttons for each application
        val icon1: ImageButton? = view.findViewById(R.id.icon1)
        val icon2: ImageButton? = view.findViewById(R.id.icon2)
        val icon3: ImageButton? = view.findViewById(R.id.icon3)
        val icon4: ImageButton? = view.findViewById(R.id.icon4)
        val icon5: ImageButton? = view.findViewById(R.id.icon5)

        // Handle icon clicks and launch respective applications
        icon1?.setOnClickListener { launchApp("com.android.car.radio") }
        icon2?.setOnClickListener { launchApp("com.example.app2") }
        icon3?.setOnClickListener { launchApp("com.android.car.settings") }
        icon4?.setOnClickListener { launchApp("com.example.app4") }
        icon5?.setOnClickListener { launchApp("com.example.app5") }

        // Schedule auto-dismiss after 5 seconds
        autoDismissHandler.postDelayed({
            dismissAllowingStateLoss()
        }, 5000)

        return view
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = activity?.packageManager?.getLaunchIntentForPackage(packageName)
            intent?.let { startActivity(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Set the dialog position to the bottom
            setGravity(Gravity.BOTTOM)
            // Set transparent background for better aesthetics
        //`    setBackgroundDrawableResource(android.R.color.transparent)
            // Adjust layout params to wrap content width and height
            val params = attributes
            //params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            params.width = (context.resources.displayMetrics.widthPixels * 0.5).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = params
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel the auto-dismiss handler if the dialog is destroyed before the timeout
        autoDismissHandler.removeCallbacksAndMessages(null)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Add any logic needed on dismissing the dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Add any logic needed on canceling the dialog
    }
}
