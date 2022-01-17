package com.example.gpssportmap

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MapDbHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "Map.db"
        const val DATABASE_VERSION = 1

        const val SESSION_TABLE_NAME = "SESSION"

        const val SESSION_ID = "_id"
        const val SESSION_API_ID = "SessionApiId"
        const val SESSION_NAME = "SessionName"
        const val DATE_SAVED = "DateSaved"
        const val TRACK_HISTORY = "TrackHistory"
        const val MARKERS_HISTORY = "MarkersHistory"

        const val SQL_SESSIONS_CREATE_TABLE =
            "CREATE TABLE $SESSION_TABLE_NAME (" +
                    "$SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$SESSION_API_ID TEXT NULL, " +
                    "$SESSION_NAME TEXT NOT NULL, " +
                    "$DATE_SAVED TEXT NOT NULL, " +
                    "$TRACK_HISTORY TEXT NOT NULL, " +
                    "$MARKERS_HISTORY TEXT NOT NULL);"

        const val SQL_DELETE_SESSION_TABLE = "DROP TABLE IF EXISTS $SESSION_TABLE_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_SESSIONS_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_SESSION_TABLE)
        onCreate(db)
    }

    fun deleteAllSessions(db: SQLiteDatabase?) {
        db?.execSQL(SQL_DELETE_SESSION_TABLE)
        db?.execSQL(SQL_SESSIONS_CREATE_TABLE)
    }
}