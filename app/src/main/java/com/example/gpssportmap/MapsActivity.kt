package com.example.gpssportmap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.gpssportmap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.*

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView

import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var binding: ActivityMapsBinding

    private var markers: MutableList<Marker> = mutableListOf()
    private var wayPoint: Marker? = null
    private var lastKnownCoordinate: LatLng? = null
    private var lastKnownDistance: Int? = null

    private var polyLineOptions = PolylineOptions().width(10f).color(Color.CYAN)
    private var polyLine: Polyline? = null

    private val intentFilter = IntentFilter()
    private val handler: Handler = Handler(Looper.getMainLooper())

    private lateinit var mapBrain: MapBrain

    private val runSessionTimer: Runnable = object : Runnable {
        override fun run() {
            findViewById<TextView>(R.id.textViewTimeSession).text =
                mapBrain.getTimeStringFromInt(mapBrain.timeSession)
            mapBrain.incrementSessionTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val runMarkerTimer: Runnable = object : Runnable {
        override fun run() {
            findViewById<TextView>(R.id.textViewTimeMarker).text =
                mapBrain.getTimeStringFromInt(mapBrain.timeMarker)
            mapBrain.incrementMarkerTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val runWaypointTimer: Runnable = object : Runnable {
        override fun run() {
            findViewById<TextView>(R.id.textViewTimeWaypoint).text =
                mapBrain.getTimeStringFromInt(mapBrain.timeWaypoint)
            mapBrain.incrementWaypointTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        intentFilter.addAction(GPSService.LOCATION_UPDATE)
    }

    private val receiver = Receiver()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapBrain = MapBrain()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
//        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        mMap = googleMap
    }

    override fun onStop() {
        super.onStop()

    }

    fun sessionButtonOnClick(view: android.view.View) {
        mapBrain.requesting = if (!mapBrain.requesting) {
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)
            val intent = Intent(this, GPSService::class.java)
            startService(intent)
            handler.removeCallbacks(runSessionTimer)
            handler.post(runSessionTimer)
            true
        } else {
            handler.removeCallbacks(runSessionTimer)
            mapBrain.resetSession()
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
            val intent = Intent(this, GPSService::class.java)
            stopService(intent)
            false
        }
    }

    fun markerButtonOnClick(view: android.view.View) {
        if (lastKnownCoordinate != null && mMap != null) {
            handler.removeCallbacks(runMarkerTimer)
            handler.post(runMarkerTimer)
            mapBrain.resetMarker()
            val options = MarkerOptions()
            options
                .position(lastKnownCoordinate!!)
                .title("marker" + markers.size)
                .icon(getBitmapIcon(applicationContext, R.drawable.ic_marker))

            val marker = mMap!!.addMarker(options)
            markers.add(marker!!)
            mapBrain.markerOn = true
            mapBrain.markerLocation = LatLng(lastKnownCoordinate!!.latitude, lastKnownCoordinate!!.longitude)
            if (lastKnownCoordinate != null && lastKnownDistance != null) {
                updateUI(lastKnownDistance!!, lastKnownCoordinate!!, false)
            }
        }
    }

    fun waypointButtonOnClick(view: android.view.View) {
        if (mMap != null && mapBrain != null) {
            if (wayPoint != null) {
                wayPoint!!.remove()
            }

            mapBrain.resetWaypoint()
            val targetCords = mMap!!.cameraPosition.target
            val options = MarkerOptions()
            options
                .position(targetCords)
                .title("waypoint")
                .icon(getBitmapIcon(applicationContext, R.drawable.ic_waypoint))

            wayPoint = mMap!!.addMarker(options)
            mapBrain.waypointOn = true
            mapBrain.waypointLocation = LatLng(targetCords.latitude, targetCords.longitude)
            handler.removeCallbacks(runWaypointTimer)
            handler.post(runWaypointTimer)
            if (lastKnownCoordinate != null && lastKnownDistance != null) {
                updateUI(lastKnownDistance!!, lastKnownCoordinate!!, false)
            }
        }
    }

    private inner class Receiver: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d("Received", "")
            val location = intent!!.extras!!.get(GPSService.LOCATION) as LatLng

            if (mMap == null) return;
            val latLng = LatLng(location.latitude, location.longitude)

            if (lastKnownCoordinate != null) {
                val distance = mapBrain.getDistance(latLng, lastKnownCoordinate!!)
                lastKnownDistance = distance
                updateUI(distance, location)
            }

            lastKnownCoordinate = latLng

            polyLineOptions.add(latLng)
            if (polyLine != null) {
                polyLine!!.remove()
            }

            polyLine = mMap!!.addPolyline(polyLineOptions)
            polyLine!!.endCap = CustomCap(getBitmapIcon(applicationContext, R.drawable.ic_arrow, 50 ,50)!!)
            val newLocation = CameraUpdateFactory.newLatLngZoom(latLng, 18f)
            mMap!!.animateCamera(newLocation)
        }
    }

    fun updateUI(distance: Int, currentLocation: LatLng, updateTraveledDistance: Boolean = true) {
        if (mapBrain.requesting) {
            if(updateTraveledDistance) mapBrain.traveledSession += distance
            mapBrain.updateSpeedSession()
            findViewById<TextView>(R.id.textViewTraveledSession).text = "${mapBrain.traveledSession} m"
            findViewById<TextView>(R.id.textViewSpeedSession).text = mapBrain.getSpeedString(mapBrain.speedSession)
        }

        if (mapBrain.markerOn) {
            mapBrain.distanceMarker = mapBrain.getDistance(mapBrain.markerLocation, currentLocation)
            if(updateTraveledDistance) mapBrain.traveledMarker += distance
            mapBrain.updateSpeedMarker()
            findViewById<TextView>(R.id.textViewDistanceMarker).text = "${mapBrain.distanceMarker} m"
            findViewById<TextView>(R.id.textViewTraveledMarker).text = "${mapBrain.traveledMarker} m"
            findViewById<TextView>(R.id.textViewSpeedMarker).text = mapBrain.getSpeedString(mapBrain.speedMarker)
        }

        if (mapBrain.waypointOn) {
            mapBrain.distanceWaypoint = mapBrain.getDistance(mapBrain.waypointLocation, currentLocation)
            if(updateTraveledDistance) mapBrain.traveledWaypoint += distance
            mapBrain.updateSpeedWaypoint()
            findViewById<TextView>(R.id.textViewDistanceWaypoint).text = "${mapBrain.distanceWaypoint} m"
            findViewById<TextView>(R.id.textViewTraveledWaypoint).text = "${mapBrain.traveledWaypoint} m"
            findViewById<TextView>(R.id.textViewSpeedWaypoint).text = mapBrain.getSpeedString(mapBrain.speedWaypoint)
        }
    }

    // Code taken from this page: https://www.geeksforgeeks.org/how-to-create-landscape-layout-in-android-studio/
    private fun getBitmapIcon(context: Context, vectorResId: Int, width: Int = 100, length: Int = 100): BitmapDescriptor? {
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
}