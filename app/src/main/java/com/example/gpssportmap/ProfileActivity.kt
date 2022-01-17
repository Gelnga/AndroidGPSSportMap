package com.example.gpssportmap

import android.content.Intent
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

    fun logOutButtonOnClick(view: android.view.View) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        with (sharedPref.edit()) {
            remove(Constants.USER_EMAIL_PREF)
            remove(Constants.USER_PASSWORD_PREF)
            remove(Constants.USER_TOKEN_PREF_KEY)
            remove(Constants.USER_FIRST_NAME_PREF)
            remove(Constants.USER_LAST_NAME_PREF)
            commit()
        }

        val intent = Intent(this, LogInActivity::class.java)
        startActivity(intent)
    }
}