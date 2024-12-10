package com.example.audioapp

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

class ImageSliderDialogFragment : DialogFragment(), SliderAdapter.OnAppClickListener {

    private var viewPager2: ViewPager2? = null

    

    companion object {
        var currentSongIndex = 2
        var slider: List<SliderItem> = emptyList()
        private var viewPagerReference: ViewPager2? = null

        fun newInstance(sliderItems: List<SliderItem>): ImageSliderDialogFragment {
            slider = sliderItems
            val fragment = ImageSliderDialogFragment()
            val args = Bundle()
            args.putParcelableArrayList("sliderItems", ArrayList(sliderItems))
            fragment.arguments = args
            return fragment
        }

        fun goToNextSong() {
            currentSongIndex = (currentSongIndex + 1) % slider.size
            viewPagerReference?.setCurrentItem(currentSongIndex, true)
        }

        fun goToPreviousSong() {
            currentSongIndex = if (currentSongIndex == 0) slider.size - 1 else currentSongIndex - 1
            viewPagerReference?.setCurrentItem(currentSongIndex, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, R.style.TransparentDialogTheme)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_image_slider, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.sliderDialog = true
         Song.sliderDialog = true

        val sliderItems = arguments?.getParcelableArrayList<SliderItem>("sliderItems") ?: emptyList()
        viewPager2 = view.findViewById(R.id.viewPagerImageSlider)
        viewPager2?.let {
            it.adapter = SliderAdapter(sliderItems, this,it)
            viewPagerReference = it // Save the reference to the companion object
            val middleItem = sliderItems.size / 2
        it.setCurrentItem(middleItem, true)
        }

        // Disable clipping of parent and children
        viewPager2?.clipToPadding = false
        viewPager2?.clipChildren = false
        viewPager2?.offscreenPageLimit = 2

        // Adjust child width using a page transformer
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(1)) // Space between items
        compositePageTransformer.addTransformer { page: View, position: Float ->
            val scale = 0.9f + (1 - kotlin.math.abs(position)) * 0.15f
            page.scaleY = scale
            page.scaleX = scale
        }
        viewPager2?.setPageTransformer(compositePageTransformer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MainActivity.sliderDialog = false
        Song.sliderDialog = false
        viewPagerReference = null // Clear the reference to avoid memory leaks
    }

    override fun onAppClick(position: Int) {
        try {
            val intent = activity?.packageManager?.getLaunchIntentForPackage(slider[position].packageName)
            intent?.let { startActivity(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
