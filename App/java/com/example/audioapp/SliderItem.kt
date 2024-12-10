package com.example.audioapp

import android.os.Parcel
import android.os.Parcelable

data class SliderItem(
    val image: Int,
    val packageName: String // Added packageName parameter
) : Parcelable {

    constructor(parcel: Parcel) : this(
        image = parcel.readInt(),
        packageName = parcel.readString() ?: "" // Read the String value from the Parcel
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(image)
        parcel.writeString(packageName) // Write the String value to the Parcel
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SliderItem> {
        override fun createFromParcel(parcel: Parcel): SliderItem {
            return SliderItem(parcel)
        }

        override fun newArray(size: Int): Array<SliderItem?> {
            return arrayOfNulls(size)
        }
    }
}
