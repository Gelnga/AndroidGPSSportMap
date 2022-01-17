package com.example.gpssportmap

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class ProfileActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val email = sharedPref.getString(Constants.USER_EMAIL_PREF, null)
        val firstName = sharedPref.getString(Constants.USER_FIRST_NAME_PREF, null)
        val lastName = sharedPref.getString(Constants.USER_LAST_NAME_PREF, null)

        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.textViewEmailProfile).text = email
        findViewById<TextView>(R.id.textViewFirstNameProfile).text = firstName
        findViewById<TextView>(R.id.textViewLastNameProfile).text = lastName
    }
}