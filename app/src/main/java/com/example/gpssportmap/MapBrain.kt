package com.example.gpssportmap

import android.location.Location
import com.google.android.gms.maps.model.LatLng

class MapBrain {
    // Session data
    var requesting = false
    var timeSession: Int = 0
    var traveledSession: Int = 0
    var speedSession: Float = 0f

    // Last marker values
    var markerOn = false
    var markerLocation: LatLng = LatLng(0.0, 0.0)
    var distanceMarker: Int = 0
    var timeMarker: Int = 0
    var traveledMarker: Int = 0
    var speedMarker: Float = 0f

    // Waypoint values
    var waypointOn = false
    var waypointLocation: LatLng = LatLng(0.0, 0.0)
    var distanceWaypoint: Int = 0
    var timeWaypoint: Int = 0
    var traveledWaypoint: Int = 0
    var speedWaypoint: Float = 0f

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
}