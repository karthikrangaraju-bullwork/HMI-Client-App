package com.bullwork.hmi_headless

import android.os.Parcel      // <-- This import is crucial for resolving 'Parcel'
import android.os.Parcelable  // <-- This import is crucial for resolving 'Parcelable'

// This class is required in the client app to match the structure expected by the AIDL interface.
// It must match the data structure (fields) of the server's version exactly.
data class CanMessageAidl(
    val id: Int = 0,
    val messageId: Int,
    val data: String,
    val timestamp: Long,
    val fields: Map<String, String> = emptyMap() // structured key/value map
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<CanMessageAidl> {
        override fun createFromParcel(parcel: Parcel): CanMessageAidl {
            val id = parcel.readInt()
            val messageId = parcel.readInt()
            val data = parcel.readString() ?: ""
            val timestamp = parcel.readLong()

            // Read the Map<String,String>
            val mapSize = parcel.readInt()
            val fields = mutableMapOf<String, String>()
            repeat(mapSize) {
                val key = parcel.readString() ?: ""
                val value = parcel.readString() ?: ""
                fields[key] = value
            }

            return CanMessageAidl(id, messageId, data, timestamp, fields)
        }

        override fun newArray(size: Int): Array<CanMessageAidl?> = arrayOfNulls(size)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(messageId)
        parcel.writeString(data)
        parcel.writeLong(timestamp)

        // Write the Map<String,String>
        parcel.writeInt(fields.size)
        fields.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeString(value)
        }
    }

    override fun describeContents() = 0
}