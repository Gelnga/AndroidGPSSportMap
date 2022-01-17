package com.example.gpssportmap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class LogInActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkIsUserLoggedIn()) {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            return
        }
        setContentView(R.layout.activity_log_in)
    }

    private fun checkIsUserLoggedIn(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPref.getString(Constants.USER_TOKEN_PREF_KEY, null) ?: return false
        return true
    }

    fun logInButtonOnClick(view: android.view.View) {
        val email = findViewById<EditText>(R.id.editTextEmail).text.toString()
        val password = findViewById<EditText>(R.id.editTextPassword).text.toString()

        val url = "https://sportmap.akaver.com/api/v1/Account/Login"
        val handler = HttpSingletonHandler.getInstance(this)

        if (password.isEmpty() || email.isEmpty()) {
            Toast.makeText(applicationContext, "Please provide every input field with information!",
                Toast.LENGTH_LONG).show()
            return
        }

        val httpRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response -> processResponse(response, email, password)
                              Log.d("response", response.toString())},
            Response.ErrorListener { error -> processError(String(error.networkResponse.data))  })
        {

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = email
                params["password"] = password

                val obj = JSONObject(params as Map<*, *>).toString()
                return obj.toByteArray()
            }
        }

        handler.addToRequestQueue(httpRequest)
    }

    private fun processResponse(response: String, email: String, password: String) {
        val responseDeserialized = JSONObject(response)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        with (sharedPref.edit()) {
            val token = responseDeserialized.get("token") as String
            Log.d("token", token)
            val firstName = responseDeserialized.get("firstName") as String
            val lastName = responseDeserialized.get("lastName") as String

            putString(Constants.USER_EMAIL_PREF, email)
            putString(Constants.USER_PASSWORD_PREF, password)
            putString(Constants.USER_TOKEN_PREF_KEY, token)
            putString(Constants.USER_FIRST_NAME_PREF, firstName)
            putString(Constants.USER_LAST_NAME_PREF, lastName)

            commit()
        }

        val intent = Intent(this, MapsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun processError(error: String) {
        val errorJson = JSONObject(error)
        val errors = errorJson.get("errors") as JSONObject
        if (!errors.isNull("Email")) {
            Toast.makeText(applicationContext, "Email address not found!",
                Toast.LENGTH_LONG).show()
        }

        if (errors.isNull("Password")) {
            Toast.makeText(applicationContext, "Password is wrong!", Toast.LENGTH_LONG).show()
        }
    }


    fun registerButtonOnClick(view: android.view.View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}