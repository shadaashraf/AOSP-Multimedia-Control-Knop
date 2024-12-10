package com.example.audioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class SliderAdapter(private val items: List<SliderItem>, private val listener: OnAppClickListener, private val viewPager2: ViewPager2) :
    RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

        interface OnAppClickListener {
        fun onAppClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slide_item_container, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val sliderItem = items[position]
        // Set up your image or content for the slider item
        holder.bind(sliderItem)
        holder.itemView.setOnClickListener { listener.onAppClick(position) }
    }

    override fun getItemCount(): Int = items.size

    inner class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView? = view.findViewById(R.id.sliderImage)

        fun bind(sliderItem: SliderItem) {
            // Load the image or data into the ImageView
            imageView?.setImageResource(sliderItem.image)  // Example for loading an image
        }
    }
}
