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

class RegisterActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
    }

    fun registerAccountButtonOnClick(view: android.view.View) {
        val password = findViewById<EditText>(R.id.editTextPasswordRegister).text.toString()
        val passwordRepeat = findViewById<EditText>(R.id.editTextRepeatPasswordRegister).text.toString()
        val email = findViewById<EditText>(R.id.editTextEmailRegister).text.toString()
        val firstName = findViewById<EditText>(R.id.editTextFirstNameRegister).text.toString()
        val lastName = findViewById<EditText>(R.id.editTextLastNameRegister).text.toString()

        if (password.isEmpty() || passwordRepeat.isEmpty() || email.isEmpty() || firstName.isEmpty()
            || lastName.isEmpty()) {
            Toast.makeText(applicationContext, "Please provide every input field with information!",
                Toast.LENGTH_LONG).show()
            return
        }

        if (password != passwordRepeat) {
            Toast.makeText(applicationContext, "Passwords doesn't match", Toast.LENGTH_LONG).show()
            return
        }

        val url = "https://sportmap.akaver.com/api/v1/Account/Register"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response -> processResponse(response, email, password, firstName, lastName) },
            Response.ErrorListener { errors -> processErrors(String(errors.networkResponse.data))
            Log.d("errors", String(errors.networkResponse.data))})
        {

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = email
                params["password"] = password
                params["firstName"] = firstName
                params["lastName"] = lastName

                val obj = JSONObject(params as Map<*, *>).toString()
                return obj.toByteArray()
            }
        }

        handler.addToRequestQueue(httpRequest)
    }

    private fun processResponse(response: String, email: String, password: String, firstName: String,
                                lastName: String) {

        val responseDeserialized = JSONObject(response)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        with (sharedPref.edit()) {
            val token = responseDeserialized.get("token") as String

            putString(Constants.USER_EMAIL_PREF, email)
            putString(Constants.USER_PASSWORD_PREF, password)
            putString(Constants.USER_TOKEN_PREF_KEY, token)
            putString(Constants.USER_FIRST_NAME_PREF, firstName)
            putString(Constants.USER_LAST_NAME_PREF, lastName)
            commit()
        }

        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    private fun processErrors(error: String) {
        val errorJson = JSONObject(error)

        if (!errorJson.isNull("messages")) {
            Toast.makeText(applicationContext, "This email is already registered!",
                Toast.LENGTH_LONG).show()
            return
        }

        val errors = errorJson.get("errors") as JSONObject

        if (!errors.isNull("Email")) {
            Toast.makeText(applicationContext, "Email address is wrong!",
                Toast.LENGTH_LONG).show()
        }

        if (!errors.isNull("Password")) {
            Toast.makeText(applicationContext, "Password is wrong! Passwords must have at least one non " +
                    "alphanumeric character (!, . etc.), at least one digit, at least one " +
                    "uppercase and should be at least 6 symbols long", Toast.LENGTH_LONG).show()
        }
    }
}