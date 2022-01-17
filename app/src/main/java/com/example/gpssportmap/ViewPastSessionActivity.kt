package com.example.gpssportmap

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class ViewPastSessionActivity: AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private val mapBrain = MapBrain()
    private var dto: SessionDto = SessionDto()

    private var polyLineOptions = PolylineOptions().width(10f).color(Color.CYAN)
    private var polyLine: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_past_session)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapPast) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val repo = MapRepository(applicationContext)
            .open()
        val extras = intent.extras
        if (extras != null) {
            val id = extras.get(Constants.SESSION_ID) as Int
            dto = repo.getSessionById(id)
        }
        repo.close()
    }


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        restoreMapDrawingsFromBrain()
    }

    fun getMarkerOptions(latLng: LatLng): MarkerOptions {
        val options = MarkerOptions()
        options
            .position(latLng)
            .title("")
            .icon(mapBrain.getBitmapIcon(applicationContext, R.drawable.ic_marker))

        return options
    }

    private fun restoreMapDrawingsFromBrain() {
        restoreMarksFromMarksHistory()
        restoreTrackFromTrackHistory()
    }

    private fun restoreMarksFromMarksHistory() {
        val restoredCords = mutableListOf<LatLng>()
        mapBrain.restoreListOfCoordinatesFromJson(restoredCords, dto.markersHistory)

        for (cord in restoredCords) {
            mMap!!.addMarker(getMarkerOptions(cord))
        }
    }

    private fun restoreTrackFromTrackHistory() {
        var lastCord = LatLng(0.0, 0.0)
        val restoredCords = mutableListOf<LatLng>()
        mapBrain.restoreListOfCoordinatesFromJson(restoredCords, dto.trackHistory)

        for (cord in restoredCords) {
            polyLineOptions.add(cord)
            lastCord = cord
        }
        polyLine = mMap!!.addPolyline(polyLineOptions)
        polyLine!!.endCap = CustomCap(mapBrain.getBitmapIcon(applicationContext, R.drawable.ic_arrow, 50 ,50)!!)
        val newLocation = CameraUpdateFactory.newLatLngZoom(lastCord, 18f)
        mMap!!.animateCamera(newLocation)
    }
}