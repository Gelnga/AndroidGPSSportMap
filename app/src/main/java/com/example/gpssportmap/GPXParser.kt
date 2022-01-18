package com.example.gpssportmap

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class GPXParser {

    fun gpxParse(trackHistory: MutableList<LatLng>, checkpointHistory: MutableList<LatLng>, context: Context) {

        var gpx = "<gpx version=\"0.6\" creator=\"Gleb Engalychev\"><trk>"

        for (cord in trackHistory) {
            gpx += "<trkpt lat=\"${cord.latitude}\" lon=\"${cord.longitude}\"></trkpt>"
        }

        gpx += "</trk>"

        for (cord in checkpointHistory) {
            gpx += "<wpt lat=\"${cord.latitude}\" lon=\"${cord.longitude}\"></wpt>"
        }

        gpx += "</gpx>"

        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS);
        val myFile = File(path, "parsed" + (0..10000).random() + ".txt");
        val fOut = FileOutputStream(myFile,true);
        val myOutWriter = OutputStreamWriter(fOut);
        myOutWriter.append(gpx);
        myOutWriter.close();
        fOut.close();

        Toast.makeText(context,"Text file Saved !", Toast.LENGTH_LONG).show();
    }

    fun restoreListOfCoordinatesFromJson(cords: MutableList<LatLng>, json: String) {
        val restoredArray = JSONArray(json)
        for (i in 0 until restoredArray.length()) {
            val restoredCord = restoredArray[i] as JSONObject
            val latitude = restoredCord.get(Constants.LATITUDE) as Double
            val longitude = restoredCord.get(Constants.LONGITUDE) as Double
            val cord = LatLng(latitude, longitude)

            cords.add(cord)
        }
    }
}