package com.example.gpssportmap

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception

class GPSService : Service(), LocationListener {

    companion object {
        const val LOCATION_UPDATE = "locationUpdate"
        const val LOCATION = "location"
    }

    private lateinit var locationManager: LocationManager
    private var provider: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        locationManager.requestLocationUpdates(provider!!, 3000, 0f, this)

        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onLocationChanged(location: Location) {
        Log.d("Changed", "")
        val latLng = LatLng(location.latitude, location.longitude)
        val intent = Intent(LOCATION_UPDATE)
        intent.putExtra(LOCATION, latLng)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}