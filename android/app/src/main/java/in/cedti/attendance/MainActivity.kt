```kotlin
package `in`.cedti.attendance

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.content.Context
import android.widget.*
import android.graphics.Typeface
import android.graphics.Color
import android.text.InputType
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val baseUrl =
        BuildConfig.BASE_URL.trimEnd('/') + "/"

    private val PREFS = "CEDTI_ATTENDANCE"

    private var staffCode = ""
    private var staffName = ""
    private var authToken = ""

    private lateinit var mainLayout: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var loginCode: EditText
    private lateinit var loginPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        staffCode = prefs.getString("staff_code", "") ?: ""
        staffName = prefs.getString("staff_name", "") ?: ""
        authToken = prefs.getString("auth_token", "") ?: ""

        if (staffCode.isNotEmpty() && authToken.isNotEmpty()) {
            showDashboard()
        } else {
            showLogin()
        }
    }

    private fun setupLayout() {
        mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(25, 30, 25, 25)
        mainLayout.setBackgroundColor(Color.WHITE)

        val scroll = ScrollView(this)
        scroll.addView(mainLayout)

        setContentView(scroll)
    }

    private fun addTitle(text: String) {
        val title = TextView(this)
        title
