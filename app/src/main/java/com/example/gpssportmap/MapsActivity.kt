package com.example.gpssportmap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.example.gpssportmap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.*

import android.graphics.Color
import android.widget.TextView

import com.google.android.gms.maps.model.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var binding: ActivityMapsBinding

    private var polyLineOptions = PolylineOptions().width(10f).color(Color.CYAN)
    private var polyLine: Polyline? = null
    private lateinit var mapBrain: MapBrain
    private var wayPoint: Marker? = null
    private var sessionStart: Boolean = false

    private val receiver = Receiver()
    private val intentFilter = IntentFilter()
    init {
        intentFilter.addAction(Constants.LOCATION_UPDATE_ACTION)
        intentFilter.addAction(Constants.SEND_MAP_BRAIN_ACTION)
        intentFilter.addAction(Constants.MARKER_CLICK_ACTION_NOT)
        intentFilter.addAction(Constants.WAYPOINT_CLICK_ACTION_NOT)
        intentFilter.addAction(Constants.SESSION_TIMER_ACTION)
        intentFilter.addAction(Constants.MARKER_TIMER_ACTION)
        intentFilter.addAction(Constants.WAYPOINT_TIMER_ACTION)
    }

    private fun updateSessionTime(time: String) {
        findViewById<TextView>(R.id.textViewTimeSession).text = time
    }

    private fun updateMarkerTime(time: String) {
        findViewById<TextView>(R.id.textViewTimeMarker).text = time
    }

    private fun updateWaypointTime(time: String) {
        findViewById<TextView>(R.id.textViewTimeWaypoint).text = time
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        sessionStart = if (!sessionStart) {
            mMap!!.clear()
            registerReceiver(receiver, intentFilter)
            val intent = Intent(this, GPSService::class.java)
            startService(intent)
            true
        } else {
            mapBrain.resetSession()
            unregisterReceiver(receiver)
            val intent = Intent(this, GPSService::class.java)
            stopService(intent)
            false
        }
    }

    fun markerButtonOnClick(View: android.view.View) {
        markerButtonClicked()
    }

    fun waypointButtonOnClick(View: android.view.View) {
        waypointButtonClicked()
    }

    private fun markerButtonClicked(sendBroadcast: Boolean = true) {
        if (mMap != null && sessionStart) {
            val options = MarkerOptions()
            options
                .position(mapBrain.lastKnownCoordinate!!)
                .title("")
                .icon(mapBrain.getBitmapIcon(applicationContext, R.drawable.ic_marker))

            val marker = mMap!!.addMarker(options)

            if (sendBroadcast) {
                val intent = Intent(Constants.MARKER_CLICK_ACTION)
                sendBroadcast(intent)
            }
        }
    }

    private fun waypointButtonClicked(sendBroadcast: Boolean = true) {
        if (sessionStart) {
            if (wayPoint != null) {
                wayPoint!!.remove()
            }

            val targetCords = mMap!!.cameraPosition.target
            val options = MarkerOptions()
            options
                .position(targetCords)
                .title("waypoint")
                .icon(mapBrain.getBitmapIcon(applicationContext, R.drawable.ic_waypoint))

            wayPoint = mMap!!.addMarker(options)

            if (sendBroadcast) {
                val intent = Intent(Constants.WAYPOINT_CLICK_ACTION)
                sendBroadcast(intent)
            }
        }
    }

    private inner class Receiver: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent!!.action == Constants.LOCATION_UPDATE_ACTION) {
                processGPSServiceBroadcast(intent)
            }

            if (intent.action == Constants.SEND_MAP_BRAIN_ACTION) {
                mapBrain = intent.extras!!.get(Constants.MAP_BRAIN) as MapBrain
            }

            if (intent.action == Constants.MARKER_CLICK_ACTION_NOT) {
                markerButtonClicked(false)
            }

            if (intent.action == Constants.WAYPOINT_CLICK_ACTION_NOT) {
                waypointButtonClicked(false)
            }

            if (intent.action == Constants.SESSION_TIMER_ACTION) {
                updateSessionTime(intent.extras!!.get(Constants.SESSION_TIME) as String)
            }

            if (intent.action == Constants.MARKER_TIMER_ACTION) {
                updateMarkerTime(intent.extras!!.get(Constants.MARKER_TIME) as String)
            }

            if (intent.action == Constants.WAYPOINT_TIMER_ACTION) {
                updateWaypointTime(intent.extras!!.get(Constants.WAYPOINT_TIME) as String)
            }
        }

        private fun processGPSServiceBroadcast(intent: Intent) {
            val location = intent.extras!!.get(Constants.LOCATION) as LatLng

            if (mMap == null) return;
            val latLng = LatLng(location.latitude, location.longitude)

            if (mapBrain.lastKnownCoordinate != null) {
                val distance = mapBrain.getDistance(latLng, mapBrain.lastKnownCoordinate!!)
                updateUI(distance, location)
            }

            polyLineOptions.add(latLng)
            if (polyLine != null) {
                polyLine!!.remove()
            }

            polyLine = mMap!!.addPolyline(polyLineOptions)
            polyLine!!.endCap = CustomCap(mapBrain.getBitmapIcon(applicationContext, R.drawable.ic_arrow, 50 ,50)!!)
            val newLocation = CameraUpdateFactory.newLatLngZoom(latLng, 18f)
            mMap!!.animateCamera(newLocation)
        }
    }

    private fun updateUI(distance: Int, currentLocation: LatLng, updateTraveledDistance: Boolean = true) {
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
}