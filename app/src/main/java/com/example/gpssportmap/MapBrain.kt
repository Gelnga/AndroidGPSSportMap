package com.example.gpssportmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.*
import org.json.JSONObject

class MapBrain() : Parcelable {
    // Session data
    var requesting = false
    var timeSession: Int = 0
    var traveledSession: Int = 0
    var speedSession: Float = 0f

    // Last marker data
    var markerOn = false
    var markerLocation: LatLng = LatLng(0.0, 0.0)
    var distanceMarker: Int = 0
    var timeMarker: Int = 0
    var traveledMarker: Int = 0
    var speedMarker: Float = 0f

    // Waypoint data
    var waypointOn = false
    var waypointLocation: LatLng = LatLng(0.0, 0.0)
    var distanceWaypoint: Int = 0
    var timeWaypoint: Int = 0
    var traveledWaypoint: Int = 0
    var speedWaypoint: Float = 0f

    // Last known coordinate related data
    var lastKnownCoordinate: LatLng? = null
    var lastKnownDistance: Int? = null

    // Data saving
    var trackHistory: MutableList<LatLng> = mutableListOf()
    var markersHistory: MutableList<LatLng> = mutableListOf()

    constructor(parcel: Parcel) : this() {
        // Session data
        requesting = parcel.readByte() != 0.toByte()
        timeSession = parcel.readInt()
        traveledSession = parcel.readInt()
        speedSession = parcel.readFloat()

        // Last marker data
        markerOn = parcel.readByte() != 0.toByte()
        markerLocation = parcel.readParcelable(LatLng::class.java.classLoader)!!
        distanceMarker = parcel.readInt()
        timeMarker = parcel.readInt()
        traveledMarker = parcel.readInt()
        speedMarker = parcel.readFloat()

        // Waypoint data
        waypointOn = parcel.readByte() != 0.toByte()
        waypointLocation = parcel.readParcelable(LatLng::class.java.classLoader)!!
        distanceWaypoint = parcel.readInt()
        timeWaypoint = parcel.readInt()
        traveledWaypoint = parcel.readInt()
        speedWaypoint = parcel.readFloat()

        // Last known coordinate related data
        lastKnownCoordinate = parcel.readParcelable(LatLng::class.java.classLoader)
        lastKnownDistance = parcel.readValue(Int::class.java.classLoader) as? Int

        // Data saving
        parcel.readList(trackHistory, this::class.java.classLoader)
        parcel.readList(markersHistory, this::class.java.classLoader)
    }

    fun addWaypoint(mapCenterCord: LatLng? = null): Boolean {
        resetWaypoint()
        waypointOn = true

        if (mapCenterCord != null) {
            waypointLocation = LatLng(mapCenterCord.latitude, mapCenterCord.longitude)
        } else if (lastKnownCoordinate != null) {
            waypointLocation = LatLng(lastKnownCoordinate!!.latitude, lastKnownCoordinate!!.longitude)
        }


        if (lastKnownCoordinate != null && lastKnownDistance != null) {
            return true
        }
        return false
    }

    fun addMarker(): Boolean {
        if (lastKnownCoordinate != null) {
            resetMarker()
            markersHistory.add(lastKnownCoordinate!!)
            markerOn = true
            markerLocation = LatLng(lastKnownCoordinate!!.latitude, lastKnownCoordinate!!.longitude)
            if (lastKnownCoordinate != null && lastKnownDistance != null) {
                return true
            }
            return false
        }
        return false
    }

    fun updateTrackHistory(latlng: LatLng) {
        trackHistory.add(latlng)
    }

    fun incrementSessionTime() {
        timeSession++
    }

    fun incrementMarkerTime() {
        timeMarker++
    }

    fun incrementWaypointTime() {
        timeWaypoint++
    }

    fun updateSpeedSession() {
        val speed = (traveledSession.toFloat() / 1000f) / (timeSession.toFloat())
        speedSession = 1f / speed
    }

    fun updateSpeedMarker() {
        val speed = (traveledMarker.toFloat() / 1000f) / (timeMarker.toFloat())
        speedMarker = 1f/speed
    }

    fun updateSpeedWaypoint() {
        val speed = (traveledWaypoint.toFloat() / 1000f) / (timeWaypoint.toFloat())
        speedWaypoint = 1f / speed
    }

    fun resetSession() {
        timeSession = 0
        traveledSession = 0
        speedSession = 0f
    }

    fun resetMarker() {
        markerLocation = LatLng(0.0, 0.0)
        distanceMarker = 0
        timeMarker = 0
        traveledMarker = 0
        speedMarker = 0f
    }

    fun resetWaypoint() {
        waypointLocation = LatLng(0.0, 0.0)
        distanceWaypoint = 0
        timeWaypoint = 0
        traveledWaypoint = 0
        speedWaypoint = 0f
    }

//    fun trackHistoryToJson() {
//        var json = ""
//        for (cord in trackHistory) {
//            json += JSON
//        }
//    }

    fun getTimeStringFromInt(time: Int): String {
        val hours = time / 3600
        val minutes = (time - hours * 3600) / 60
        val seconds = time - minutes * 60 - hours * 3600


        return "${getTimeUnitString(hours)}:" +
                "${getTimeUnitString(minutes)}:" +
                getTimeUnitString(seconds)
    }

    private fun getTimeUnitString(timeUnit: Int): String {
        if (timeUnit < 10) {
            return "0$timeUnit"
        }
        return timeUnit.toString()
    }

    fun getDistance(l1: LatLng, l2: LatLng): Int {
        val results = FloatArray(2)
        Location.distanceBetween(l1.latitude, l1.longitude, l2.latitude, l2.longitude, results)
        return results[0].toInt()
    }

    fun getSpeedString(speed: Float): String {
        val speedAsInt = speed.toInt()
        val minutes = speedAsInt / 60
        val seconds = speedAsInt - minutes * 60

        return "$minutes:" + getTimeUnitString(seconds) + " min/km"
    }

    // Code taken from this page: https://www.geeksforgeeks.org/how-to-create-landscape-layout-in-android-studio/
    fun getBitmapIcon(context: Context, vectorResId: Int, width: Int = 100, length: Int = 100): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            width,
            length
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            width,
            length,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Session data
        parcel.writeByte(if (requesting) 1 else 0)
        parcel.writeInt(timeSession)
        parcel.writeInt(traveledSession)
        parcel.writeFloat(speedSession)

        // Last marker data
        parcel.writeByte(if (markerOn) 1 else 0)
        parcel.writeParcelable(markerLocation, flags)
        parcel.writeInt(distanceMarker)
        parcel.writeInt(timeMarker)
        parcel.writeInt(traveledMarker)
        parcel.writeFloat(speedMarker)

        // Last waypoint data
        parcel.writeByte(if (waypointOn) 1 else 0)
        parcel.writeParcelable(waypointLocation, flags)
        parcel.writeInt(distanceWaypoint)
        parcel.writeInt(timeWaypoint)
        parcel.writeInt(traveledWaypoint)
        parcel.writeFloat(speedWaypoint)

        // Last known coordinate data
        parcel.writeParcelable(lastKnownCoordinate, flags)
        parcel.writeValue(lastKnownDistance)

        // Data saving
        parcel.writeList(trackHistory)
        parcel.writeList(markersHistory)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MapBrain> {
        override fun createFromParcel(parcel: Parcel): MapBrain {
            return MapBrain(parcel)
        }

        override fun newArray(size: Int): Array<MapBrain?> {
            return arrayOfNulls(size)
        }
    }
}