package com.example.gpssportmap

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper

//class MapDbHelper(context: Context): SQLiteOpenHelper() {
//    companion object {
//        const val DATABASE_NAME = "Map.db"
//        const val DATABASE_VERSION = 0
//
//        const val MAP_BRAIN_TABLE_NAME = "MAP_BRAIN"
//

//
//
//        const val SAVED_GAME_ID = "_id"
//        const val SAVED_STATE = "SavedState"
//        const val SAVE_DATE = "SavingDate"
//        const val GAME_NAME = "GameName"
//
//        const val LEADERBOARD_TABLE_NAME = "LEADERBOARD"
//
//        const val GAME_ID = "_id"
//        const val PLAYER_NAME = "PlayerName"
//        const val TIME_SPENT = "TimeSpent"
//        const val MOVES_MADE = "MovesMade"
//
//        const val SQL_GAME_SAVES_CREATE_TABLE =
//            "CREATE TABLE $GAME_SAVES_TABLE_NAME (" +
//                    "$SAVED_GAME_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                    "$SAVED_STATE TEXT NOT NULL, " +
//                    "$SAVE_DATE TEXT NOT NULL, " +
//                    "$GAME_NAME TEXT NOT NULL);"
//
//        const val SQL_DELETE_GAME_SAVES_TABLES = "DROP TABLE IF EXISTS $GAME_SAVES_TABLE_NAME"
//
//        const val SQL_GAME_LEADERBOARD_CREATE_TABLE =
//            "CREATE TABLE $LEADERBOARD_TABLE_NAME (" +
//                    "$GAME_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                    "$PLAYER_NAME TEXT NOT NULL, " +
//                    "$TIME_SPENT TEXT NOT NULL, " +
//                    "$MOVES_MADE INTEGER NOT NULL);"
//
//        const val SQL_DELETE_LEADERBOARD_TABLES = "DROP TABLE IF EXISTS $LEADERBOARD_TABLE_NAME"
//    }
//}