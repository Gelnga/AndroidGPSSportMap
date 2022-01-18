package com.example.gpssportmap

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

class MapRepository(private val context: Context) {
    private lateinit var dbHelper: MapDbHelper
    private lateinit var db: SQLiteDatabase
    private val stf = SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.ENGLISH)

    fun open(): MapRepository {
        dbHelper = MapDbHelper(context)
        db = dbHelper.writableDatabase
        return this
    }

    fun close() {
        dbHelper.close()
    }

    fun saveSession(mapBrain: MapBrain, name: String, sessionId: String) {
        val contentValues = ContentValues()

        contentValues.put(MapDbHelper.SESSION_NAME, name)
        contentValues.put(MapDbHelper.SESSION_API_ID, sessionId)
        contentValues.put(MapDbHelper.DATE_SAVED, stf.format(Calendar.getInstance().time).toString())
        contentValues.put(MapDbHelper.TRACK_HISTORY, mapBrain.getTrackHistoryJson())
        contentValues.put(MapDbHelper.MARKERS_HISTORY, mapBrain.getMarkersHistoryJson())

        db.insert(MapDbHelper.SESSION_TABLE_NAME, null, contentValues)
    }

    fun updateSession(mapBrain: MapBrain, name: String) {
        val contentValues = ContentValues()

        contentValues.put(MapDbHelper.DATE_SAVED, stf.format(Calendar.getInstance().time).toString())
        contentValues.put(MapDbHelper.TRACK_HISTORY, mapBrain.getTrackHistoryJson())
        contentValues.put(MapDbHelper.MARKERS_HISTORY, mapBrain.getMarkersHistoryJson())

        db.update(MapDbHelper.SESSION_TABLE_NAME, contentValues, "${MapDbHelper.SESSION_NAME}=?", arrayOf(name))
    }

    fun getSessions(): Array<SessionDto?> {
        val cursor = db.query(MapDbHelper.SESSION_TABLE_NAME,
        null,
        null,
        null,
        null,
        null,
        null)

        val sessions = arrayOfNulls<SessionDto>(cursor.count)
        for (i in 0 until cursor.count) {
            val sessionDto = SessionDto()
            cursor.moveToNext()
            sessionDto.sessionId = cursor.getInt(0)
            sessionDto.sessionApiId = cursor.getString(1)
            sessionDto.sessionName = cursor.getString(2)
            sessionDto.dateSaved = cursor.getString(3)
            sessionDto.trackHistory = cursor.getString(4)
            sessionDto.markersHistory = cursor.getString(5)

            sessions[i] = sessionDto
        }

        cursor.close()
        return sessions
    }

    fun getSessionById(id :Int): SessionDto {
        val session = db.rawQuery("SELECT * FROM " + MapDbHelper.SESSION_TABLE_NAME +
                " WHERE " + MapDbHelper.SESSION_ID + "=" + id.toString(), null)
        val sessionDto = SessionDto()
        session.moveToNext()

        sessionDto.sessionId = session.getInt(0)
        sessionDto.sessionApiId = session.getString(1)
        sessionDto.sessionName = session.getString(2)
        sessionDto.dateSaved = session.getString(3)
        sessionDto.trackHistory = session.getString(4)
        sessionDto.markersHistory = session.getString(5)

        session.close()

        return sessionDto
    }

    fun deleteSession(id: Int) {
        db.delete(MapDbHelper.SESSION_TABLE_NAME, "_id=?", arrayOf(id.toString()))
    }

    fun deleteAllSessions() {
        dbHelper.deleteAllSessions(db)
    }
}