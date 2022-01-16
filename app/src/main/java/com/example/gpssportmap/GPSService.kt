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
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception

class GPSService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var mapBrain: MapBrain
    private var provider: String? = null

    private var notificationView: RemoteViews? = null
    private var builder: NotificationCompat.Builder? = null

    private val receiver = Receiver()
    private val intentFilter = IntentFilter()
    init {
        intentFilter.addAction(Constants.MARKER_CLICK_ACTION)
        intentFilter.addAction(Constants.WAYPOINT_CLICK_ACTION)
        intentFilter.addAction(Constants.ADD_MARKER_ACTION)
        intentFilter.addAction(Constants.ADD_WAYPOINT_ACTION)
    }

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

                handler.postDelayed(this, 1000)
            }
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

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isDead = false
        mapBrain = MapBrain()
        mapBrain.requesting = true
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
        handler.post(runSessionTimer)

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

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
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

        sendBroadcast(intentBrain)
        sendBroadcast(intentLoc)
    }

    fun markerButtonOnClick(sendBroadcast: Boolean = true) {
        handler.removeCallbacks(runMarkerTimer)
        handler.post(runMarkerTimer)
        if (mapBrain.addMarker()) {
            updateNotificationUi(mapBrain.lastKnownDistance!!, mapBrain.lastKnownCoordinate!!, false)
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
        }

    }
}