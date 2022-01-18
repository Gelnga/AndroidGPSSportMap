package com.example.gpssportmap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.lang.Exception
import kotlin.collections.HashMap
import kotlin.random.Random

class GPSService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var mapBrain: MapBrain
    private var provider: String? = null

    private var notificationView: RemoteViews? = null
    private var builder: NotificationCompat.Builder? = null

    private var autosaving: Boolean = false
    private var autosaveName: String? = null

    private var sessionId: String? = null
    private var queuedTrackHistory: MutableList<Location> = mutableListOf()
    private var queuedMarkers: MutableList<Location> = mutableListOf()
    private var lastKnownLocation: Location? = null
    private lateinit var token: String
    private var previousWaypointId: String? = null

    private val receiver = Receiver()
    private val intentFilter = IntentFilter()
    init {
        intentFilter.addAction(Constants.MARKER_CLICK_ACTION)
        intentFilter.addAction(Constants.WAYPOINT_CLICK_ACTION)
        intentFilter.addAction(Constants.ADD_MARKER_ACTION)
        intentFilter.addAction(Constants.ADD_WAYPOINT_ACTION)
        intentFilter.addAction(Constants.ASK_FOR_CACHED_BRAIN_ACTION)
        intentFilter.addAction(Constants.SAVE_COMPLETED_SESSION_ACTION)
    }

    private lateinit var mapRepository: MapRepository

    private var isDead: Boolean = true

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val runSessionTimer: Runnable = object : Runnable {
        override fun run() {
            val time = mapBrain.getTimeStringFromInt(mapBrain.timeSession)
            mapBrain.incrementSessionTime()

            val intent = Intent(Constants.SESSION_TIMER_ACTION)
            intent.putExtra(Constants.SESSION_TIME, time)
            sendBroadcast(intent)

            handler.postDelayed(this, 1000)
        }
    }

    private val runMarkerTimer: Runnable = object : Runnable {
        override fun run() {
            if (mapBrain.requesting) {
                val time = mapBrain.getTimeStringFromInt(mapBrain.timeMarker)
                mapBrain.incrementMarkerTime()
                notificationView!!.setTextViewText(R.id.textViewTimeMarkerNot, time)

                builder!!.setContent(notificationView)
                startForeground(Constants.MAIN_NOTIFICATION_ID, builder!!.build())

                val intent = Intent(Constants.MARKER_TIMER_ACTION)
                intent.putExtra(Constants.MARKER_TIME, time)
                sendBroadcast(intent)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val runWaypointTimer: Runnable = object : Runnable {
        override fun run() {
            if(mapBrain.requesting) {
                val time = mapBrain.getTimeStringFromInt(mapBrain.timeWaypoint)
                mapBrain.incrementWaypointTime()
                notificationView!!.setTextViewText(R.id.textViewTimeWaypointNot, time)

                builder!!.setContent(notificationView)
                startForeground(Constants.MAIN_NOTIFICATION_ID, builder!!.build())

                val intent = Intent(Constants.WAYPOINT_TIMER_ACTION)
                intent.putExtra(Constants.WAYPOINT_TIME, time)
                sendBroadcast(intent)

            }
            handler.postDelayed(this, 1000)
        }
    }

    private val backendSyncTimer: Runnable = object : Runnable {
        override fun run() {
            if (sessionId != null) {
                val toRemove = mutableListOf<Location>()
                for (location in queuedTrackHistory) {
                    sendCoordinateToBackend(Constants.GPS_LOCATION_TYPE_LOC_ID, location)
                    toRemove.add(location)
                }

                for (location in toRemove) {
                    queuedTrackHistory.remove(location)
                }

                for (checkpoint in queuedMarkers) {
                    sendCoordinateToBackend(Constants.GPS_LOCATION_TYPE_CP_ID, checkpoint)
                    toRemove.add(checkpoint)
                }

                for (location in toRemove) {
                    queuedMarkers.remove(location)
                }

                if (mapBrain.waypointLocation.latitude != 0.0 && mapBrain.waypointLocation.longitude != 0.0)
                {
                    sendCoordinateToBackend(Constants.GPS_LOCATION_TYPE_WP_ID, null, mapBrain.waypointLocation)
                }
            }
            handler.postDelayed(this, 15000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isDead = false
        mapBrain = MapBrain()
        mapBrain.requesting = true
        mapRepository = MapRepository(applicationContext)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        token = sharedPref.getString(Constants.USER_TOKEN_PREF_KEY, null)!!
        createNewSessionInBackend()

        val criteria = Criteria()
        provider = locationManager.getBestProvider(criteria, true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw Exception("No access")
        }

        val intentBrain = Intent(Constants.SEND_MAP_BRAIN_ACTION)
        intentBrain.putExtra(Constants.MAP_BRAIN, mapBrain)
        sendBroadcast(intentBrain)

        handler.removeCallbacks(runSessionTimer)
        handler.removeCallbacks(backendSyncTimer)
        handler.post(runSessionTimer)
        handler.post(backendSyncTimer)

        locationManager.requestLocationUpdates(provider!!, 3000, 0f, this)

        registerReceiver(receiver, intentFilter)
        createNotificationChannel()
        createNotification()

        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        isDead = true
        unregisterReceiver(receiver)
        handler.removeCallbacks(runSessionTimer)
        handler.removeCallbacks(runMarkerTimer)
        handler.removeCallbacks(runWaypointTimer)
        handler.removeCallbacks(backendSyncTimer)

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        queuedTrackHistory.add(location)
        lastKnownLocation = location

        mapBrain.updateTrackHistory(latLng)
        val intentLoc = Intent(Constants.LOCATION_UPDATE_ACTION)
        intentLoc.putExtra(Constants.LOCATION, latLng)

        if (mapBrain.lastKnownCoordinate != null) {
            val distance = mapBrain.getDistance(latLng, mapBrain.lastKnownCoordinate!!)
            mapBrain.lastKnownDistance = distance

            if (mapBrain.requesting) mapBrain.traveledSession += distance
            mapBrain.updateSpeedSession()
            updateNotificationUi(distance, latLng)
        }

        mapBrain.lastKnownCoordinate = latLng
        val intentBrain = Intent(Constants.SEND_MAP_BRAIN_ACTION)
        intentBrain.putExtra(Constants.MAP_BRAIN, mapBrain)

        if (!autosaving) {
            autosaving = true
            autosaveName = "Autosave" + (0..10000).random()
            mapRepository.open()
            mapRepository.saveSession(mapBrain, autosaveName!! ,sessionId!!)
            mapRepository.close()
        }

        mapRepository.open()
        mapRepository.updateSession(mapBrain, autosaveName!!)
        mapRepository.close()

        sendBroadcast(intentBrain)
        sendBroadcast(intentLoc)
    }

    fun markerButtonOnClick(sendBroadcast: Boolean = true) {
        handler.removeCallbacks(runMarkerTimer)
        handler.post(runMarkerTimer)
        if (mapBrain.addMarker()) {
            updateNotificationUi(mapBrain.lastKnownDistance!!, mapBrain.lastKnownCoordinate!!, false)
            if (lastKnownLocation != null) {
                queuedMarkers.add(lastKnownLocation!!)
            }
        }

        if (sendBroadcast) {
            val intent = Intent(Constants.MARKER_CLICK_ACTION_NOT)
            sendBroadcast(intent)
        }
    }

    fun waypointButtonOnClick(sendBroadcast: Boolean = true, sentCords: LatLng? = null) {
        handler.removeCallbacks(runWaypointTimer)
        handler.post(runWaypointTimer)
        if (mapBrain.addWaypoint(sentCords)) {
            if (sentCords != null)
            updateNotificationUi(mapBrain.lastKnownDistance!!, mapBrain.lastKnownCoordinate!!, false)
        }

        if (sendBroadcast) {
            val intent = Intent(Constants.WAYPOINT_CLICK_ACTION_NOT)
            sendBroadcast(intent)
        }
    }

    private fun updateNotificationUi(distance: Int, currentLocation: LatLng, updateTraveledDistance: Boolean = true) {
        if (mapBrain.markerOn) {
            mapBrain.distanceMarker = mapBrain.getDistance(mapBrain.markerLocation, currentLocation)
            if(updateTraveledDistance) mapBrain.traveledMarker += distance
            mapBrain.updateSpeedMarker()
            notificationView!!.setTextViewText(R.id.textViewDistanceMarkerNot, "${mapBrain.distanceMarker} m")
            notificationView!!.setTextViewText(R.id.textViewSpeedMarkerNot, mapBrain.getSpeedString(mapBrain.speedMarker))
        }

        if (mapBrain.waypointOn) {
            mapBrain.distanceWaypoint = mapBrain.getDistance(mapBrain.waypointLocation, currentLocation)
            if(updateTraveledDistance) mapBrain.traveledWaypoint += distance
            mapBrain.updateSpeedWaypoint()
            notificationView!!.setTextViewText(R.id.textViewDistanceWaypointNot, "${mapBrain.distanceWaypoint} m")
            notificationView!!.setTextViewText(R.id.textViewSpeedWaypointNot, mapBrain.getSpeedString(mapBrain.speedWaypoint))
        }

        updateNotificationView()
    }

    private fun updateNotificationView() {
        val builder = this.builder!!.setContent(notificationView)
        NotificationManagerCompat.from(this).notify(Constants.MAIN_NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.APP_CHANNEL_ID,
                Constants.APP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description = "Channel for controlling service"
            val notificationManager = getNotificationManager()
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createNotification() {
        notificationView = RemoteViews(packageName, R.layout.notification_interface)

        val markerIntent = Intent(Constants.ADD_MARKER_ACTION)
        val pendingMarkerIntent = PendingIntent.getBroadcast(this, 0, markerIntent, 0)

        val waypointIntent = Intent(Constants.ADD_WAYPOINT_ACTION)
        val pendingWaypointIntent = PendingIntent.getBroadcast(this, 0, waypointIntent, 0)

        notificationView!!.setOnClickPendingIntent(R.id.markButtonNot, pendingMarkerIntent)
        notificationView!!.setOnClickPendingIntent(R.id.wayPointButtonNot, pendingWaypointIntent)

        val builder = NotificationCompat.Builder(this, Constants.APP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_map)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContent(notificationView)

        this.builder = builder

        startForeground(Constants.MAIN_NOTIFICATION_ID, builder.build())
    }

    private inner class Receiver: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent!!.action

            if (action == Constants.ADD_MARKER_ACTION) {
                markerButtonOnClick()
            }

            if (action == Constants.ADD_WAYPOINT_ACTION) {
                waypointButtonOnClick()
            }

            if (action == Constants.MARKER_CLICK_ACTION) {
                markerButtonOnClick(false)
            }

            if (action == Constants.WAYPOINT_CLICK_ACTION) {
                val target = intent.extras!!.get(Constants.WAYPOINT_BROADCAST_VALUE) as LatLng
                waypointButtonOnClick(false, target)
            }

            if (action == Constants.ASK_FOR_CACHED_BRAIN_ACTION) {
                if (!isDead) {
                    val intentBrain = Intent(Constants.SEND_MAP_BRAIN_ACTION)
                    intentBrain.putExtra(Constants.MAP_BRAIN, mapBrain)

                    sendBroadcast(intentBrain)
                }
            }

            if (action == Constants.SAVE_COMPLETED_SESSION_ACTION) {
                val sessionName = intent.extras!!.get(Constants.COMPLETED_SESSION_NAME) as String
                mapRepository.open()
                mapRepository.saveSession(mapBrain, sessionName, sessionId!!)
                mapRepository.close()
            }
        }
    }

    private fun createNewSessionInBackend() {
        val url = "https://sportmap.akaver.com/api/v1/GpsSessions"
        val handler = HttpSingletonHandler.getInstance(this)
        val headers = mutableMapOf<String, String>()
        headers.put("Authorization", "Bearer $token")

        val httpRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response -> processResponseNewSession(response)
                Log.d("responseNewSession", response.toString())},
            Response.ErrorListener { errors -> Log.d("errorsNewSession", String(errors.networkResponse.data)) })
        {
            override fun getHeaders(): MutableMap<String, String> = headers

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()

                params["name"] = "sessionSync"
                params["description"] = "Session synchronization with server"

                val json = JSONObject(params as Map<*, *>)
                json.put("paceMin", 420)
                json.put("paceMax", 600)
                val obj = json.toString()

                return obj.toByteArray()
            }
        }

        handler.addToRequestQueue(httpRequest)
    }

    private fun processResponseNewSession(response: String) {
        val responseJson = JSONObject(response)
        sessionId = responseJson.get("id") as String
        Log.d("sesss", sessionId!!)
    }

    private fun sendCoordinateToBackend(coordinateTypeId: String, location: Location? = null, latLng: LatLng? = null) {
        val url = "https://sportmap.akaver.com/api/v1/GpsLocations"
        val handler = HttpSingletonHandler.getInstance(this)
        val headers = mutableMapOf<String, String>()
        headers.put("Authorization", "Bearer $token")

        var latitude = 0.0
        var longitude = 0.0
        var accuracy = 0f
        var altitude = 0.0

        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude
            accuracy = location.accuracy
            altitude = location.altitude
        } else {
            latitude = latLng!!.latitude
            longitude = latLng.longitude
        }

        val httpRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response -> processResponseNewCoordinate(response, coordinateTypeId)
                Log.d("responseNewCoordinate", response.toString())},
            Response.ErrorListener { errors -> Log.d("errorsNewCoordinate", String(errors.networkResponse.data)) })
        {
            override fun getHeaders(): MutableMap<String, String> = headers

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["gpsSessionId"] = sessionId!!
                params["gpsLocationTypeId"] = coordinateTypeId

                val json = JSONObject(params as Map<*, *>)
                json.put("latitude", latitude)
                json.put("longitude", longitude)
                json.put("accuracy", accuracy)
                json.put("altitude", altitude)
                val obj = json.toString()

                return obj.toByteArray()
            }
        }

        handler.addToRequestQueue(httpRequest)
    }

    private fun processResponseNewCoordinate(response: String, coordinateTypeId: String) {
        if (coordinateTypeId == Constants.GPS_LOCATION_TYPE_WP_ID) {
            val responseJson = JSONObject(response)
            val id = responseJson.get("id") as String
            if (previousWaypointId != null) {
                deleteGpsLocationById(previousWaypointId!!)
            }
            previousWaypointId = id
        }
    }

    private fun deleteGpsLocationById(id: String) {
        val url = "https://sportmap.akaver.com/api/v1/GpsLocations/$id"
        val handler = HttpSingletonHandler.getInstance(this)
        val headers = mutableMapOf<String, String>()
        headers.put("Authorization", "Bearer $token")

        val httpRequest = object : StringRequest(
            Method.DELETE,
            url,
            Response.Listener { response -> Log.d("responseDelete", response.toString())},
            Response.ErrorListener { errors -> Log.d("errorsDelete", String(errors.networkResponse.data)) })
        {
            override fun getHeaders(): MutableMap<String, String> = headers
        }

        handler.addToRequestQueue(httpRequest)
    }
}