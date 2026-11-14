package in.cedti.attendance

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManagerpackage `in`.cedti.attendance

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.content.Context
import android.widget.*
import android.view.View
import android.graphics.Typeface
import android.graphics.Color
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

    private lateinit var mainLayout: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var loginCode: EditText
    private lateinit var loginPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        staffCode = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("staff_code", "") ?: ""

        staffName = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("staff_name", "") ?: ""

        if (staffCode.isNotEmpty()) {
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
        title.text = text
        title.textSize = 23f
        title.setTextColor(Color.rgb(20, 65, 120))
        title.typeface = Typeface.DEFAULT_BOLD
        title.setPadding(0, 15, 0, 20)
        mainLayout.addView(title)
    }

    private fun addText(text: String, size: Float = 16f) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = size
        tv.setTextColor(Color.BLACK)
        tv.setPadding(0, 10, 0, 10)
        mainLayout.addView(tv)
    }

    private fun addButton(
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this)
        button.text = text
        button.isAllCaps = false
        button.setOnClickListener { action() }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 10, 0, 10)

        mainLayout.addView(button, params)
    }

    private fun addInput(
        hint: String,
        password: Boolean = false
    ): EditText {
        val input = EditText(this)
        input.hint = hint
        input.textSize = 16f

        if (password) {
            input.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        mainLayout.addView(input)
        return input
    }

    private fun showLogin() {
        setupLayout()

        addTitle("CEDTI STAFF ATTENDANCE")
        addText("Staff Login")

        loginCode = addInput("Enter Staff Code")
        loginPassword = addInput("Enter Password", true)

        statusText = TextView(this)
        statusText.textSize = 15f
        statusText.setTextColor(Color.RED)
        mainLayout.addView(statusText)

        addButton("LOGIN") {
            val code = loginCode.text.toString().trim()
            val password = loginPassword.text.toString()

            if (code.isEmpty() || password.isEmpty()) {
                statusText.text = "Please enter Staff Code and Password"
            } else {
                login(code, password)
            }
        }

        addText("CEDTI Institute of Management & Technology", 13f)
    }

    private fun showDashboard() {
        setupLayout()

        addTitle("CEDTI STAFF ATTENDANCE")

        addText("Welcome, $staffName")
        addText("Staff Code: $staffCode")

        statusText = TextView(this)
        statusText.textSize = 15f
        statusText.setTextColor(Color.rgb(0, 100, 0))
        mainLayout.addView(statusText)

        addButton("MARK ATTENDANCE IN") {
            markAttendance("IN")
        }

        addButton("MARK ATTENDANCE OUT") {
            markAttendance("OUT")
        }

        addButton("ATTENDANCE HISTORY") {
            loadHistory()
        }

        addButton("LOGOUT") {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            staffCode = ""
            staffName = ""
            showLogin()
        }

        addText("CEDTI Institute of Management & Technology", 13f)
    }

    private fun login(code: String, password: String) {
        statusText.text = "Please wait..."

        thread {
            try {
                val data = JSONObject()
                data.put("staff_code", code)
                data.put("password", password)

                val response = postRequest("api/login.php", data)

                runOnUiThread {
                    if (response.optBoolean("success")) {
                        staffCode = code
                        staffName = response.optString(
                            "name",
                            response.optString("staff_name", "Staff")
                        )

                        getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString("staff_code", staffCode)
                            .putString("staff_name", staffName)
                            .apply()

                        showDashboard()
                    } else {
                        statusText.text = response.optString(
                            "message",
                            "Login failed"
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun markAttendance(type: String) {
        if (!hasLocationPermission()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
            statusText.text = "Please allow location permission"
            return
        }

        val location = getCurrentLocation()

        if (location == null) {
            statusText.text =
                "Location not available. Please enable GPS."
            return
        }

        statusText.text = "Submitting attendance..."

        thread {
            try {
                val data = JSONObject()
                data.put("staff_code", staffCode)
                data.put("type", type)
                data.put("latitude", location.latitude)
                data.put("longitude", location.longitude)
                data.put("accuracy", location.accuracy)
                data.put(
                    "device_time",
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                )

                val response = postRequest(
                    "api/attendance.php",
                    data
                )

                runOnUiThread {
                    statusText.text = response.optString(
                        "message",
                        if (response.optBoolean("success"))
                            "Attendance submitted"
                        else
                            "Attendance failed"
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun loadHistory() {
        statusText.text = "Loading attendance history..."

        thread {
            try {
                val data = JSONObject()
                data.put("staff_code", staffCode)

                val response = postRequest(
                    "api/history.php",
                    data
                )

                runOnUiThread {
                    mainLayout.removeAllViews()

                    addTitle("ATTENDANCE HISTORY")
                    addButton("BACK TO DASHBOARD") {
                        showDashboard()
                    }

                    if (!response.optBoolean("success")) {
                        addText(
                            response.optString(
                                "message",
                                "Unable to load history"
                            )
                        )
                        return@runOnUiThread
                    }

                    val records = response.optJSONArray("records")
                        ?: response.optJSONArray("data")
                        ?: JSONArray()

                    if (records.length() == 0) {
                        addText("No attendance records found.")
                    }

                    for (i in 0 until records.length()) {
                        val item = records.optJSONObject(i)
                            ?: continue

                        val date = item.optString(
                            "server_time",
                            item.optString("date", "")
                        )

                        val type = item.optString("type", "")
                        val gps = item.optString("latitude", "")

                        addText(
                            "Date: $date\n" +
                            "Attendance: $type\n" +
                            "Latitude: $gps\n" +
                            "--------------------------"
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentLocation(): Location? {
        try {
            val manager = getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

            val gpsEnabled = manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

            val networkEnabled = manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )

            if (gpsEnabled) {
                val location = manager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )
                if (location != null) return location
            }

            if (networkEnabled) {
                return manager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )
            }

        } catch (e: Exception) {
            return null
        }

        return null
    }

    private fun postRequest(
        endpoint: String,
        data: JSONObject
    ): JSONObject {

        val url = URL(baseUrl + endpoint)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.doOutput = true

        connection.setRequestProperty(
            "Content-Type",
            "application/json; charset=UTF-8"
        )

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )

        connection.outputStream.use {
            it.write(data.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode

        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseText = stream.bufferedReader().use {
            it.readText()
        }

        connection.disconnect()

        return JSONObject(responseText)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                statusText.text =
                    "Location permission granted. Please mark attendance again."
            } else {
                statusText.text =
                    "Location permission denied."
            }
        }
    }
}
import android.widget.*
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.Color
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private var token: String? = null

    private lateinit var status: TextView
    private lateinit var staffCode: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var inButton: Button
    private lateinit var outButton: Button
    private lateinit var historyButton: Button
    private lateinit var historyView: TextView
    private lateinit var form: LinearLayout

    private val baseUrl =
        in.cedti.attendance.BuildConfig.BASE_URL.trimEnd('/') + "/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "CEDTI Staff Attendance"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 24)
            setBackgroundColor(Color.rgb(243, 246, 251))
        }

        val scroll = ScrollView(this)

        form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleView = TextView(this).apply {
            text = "CEDTI STAFF ATTENDANCE"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(18, 61, 112))
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 10)
        }

        val subtitle = TextView(this).apply {
            text = "Staff Check-in / Check-out"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        }

        staffCode = EditText(this).apply {
            hint = "Staff ID / Staff Code"
            setSingleLine(true)
        }

        password = EditText(this).apply {
            hint = "Password"
            inputType = 129
            setSingleLine(true)
        }

        loginButton = Button(this).apply {
            text = "LOGIN"
            setOnClickListener { doLogin() }
        }

        inButton = Button(this).apply {
            text = "MARK IN"
            isEnabled = false
            setOnClickListener { mark("IN") }
        }

        outButton = Button(this).apply {
            text = "MARK OUT"
            isEnabled = false
            setOnClickListener { mark("OUT") }
        }

        historyButton = Button(this).apply {
            text = "MY ATTENDANCE HISTORY"
            isEnabled = false
            setOnClickListener { loadHistory() }
        }

        val logoutButton = Button(this).apply {
            text = "LOG OUT"

            setOnClickListener {
                token = null

                inButton.isEnabled = false
                outButton.isEnabled = false
                historyButton.isEnabled = false

                loginButton.isEnabled = true
                staffCode.isEnabled = true
                password.isEnabled = true

                historyView.text = ""
                status.text = "You are logged out."
            }
        }

        status = TextView(this).apply {
            text = "Login with your CEDTI staff credentials."
            textSize = 15f
            setTextColor(Color.rgb(35, 53, 75))
            setPadding(0, 18, 0, 12)
        }

        historyView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(35, 53, 75))
            setPadding(0, 8, 0, 18)
        }

        form.addView(titleView)
        form.addView(subtitle)
        form.addView(staffCode)
        form.addView(password)
        form.addView(loginButton)
        form.addView(inButton)
        form.addView(outButton)
        form.addView(historyButton)
        form.addView(logoutButton)
        form.addView(status)
        form.addView(historyView)

        scroll.addView(form)
        root.addView(scroll)

        setContentView(root)

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        }
    }

    private fun request(
        method: String,
        path: String,
        payload: JSONObject? = null,
        auth: String? = null,
        done: (JSONObject) -> Unit
    ) {
        thread {
            var connection: HttpURLConnection? = null

            try {
                connection =
                    (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {

                        requestMethod = method
                        connectTimeout = 20000
                        readTimeout = 20000

                        setRequestProperty(
                            "Accept",
                            "application/json"
                        )

                        if (payload != null) {
                            setRequestProperty(
                                "Content-Type",
                                "application/json; charset=utf-8"
                            )
                            doOutput = true
                        }

                        if (auth != null) {
                            setRequestProperty(
                                "Authorization",
                                "Bearer $auth"
                            )
                        }
                    }

                if (payload != null) {
                    connection.outputStream.use {
                        it.write(
                            payload.toString().toByteArray(Charsets.UTF_8)
                        )
                    }
                }

                val code = connection.responseCode

                val stream =
                    if (code in 200..399) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText =
                    stream?.bufferedReader()?.use { it.readText() } ?: "{}"

                val result = JSONObject(responseText)

                runOnUiThread {
                    done(result)
                }

            } catch (e: Exception) {

                runOnUiThread {
                    status.text =
                        "Connection error: ${e.localizedMessage ?: "Please try again"}"
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun doLogin() {

        val code = staffCode.text.toString().trim()
        val pass = password.text.toString()

        if (code.isEmpty() || pass.isEmpty()) {
            status.text = "Please enter Staff Code and Password."
            return
        }

        loginButton.isEnabled = false
        status.text = "Signing in..."

        val body = JSONObject()
            .put("staff_code", code)
            .put("password", pass)

        request("POST", "api/login.php", body) { response ->

            loginButton.isEnabled = true

            if (response.optBoolean("ok")) {

                token = response.optString("token")

                val staff =
                    response.optJSONObject("staff") ?: JSONObject()

                status.text =
                    "Welcome ${staff.optString("name")}\nBranch: ${staff.optString("branch")}"

                inButton.isEnabled = true
                outButton.isEnabled = true
                historyButton.isEnabled = true

                staffCode.isEnabled = false
                password.isEnabled = false
                password.text.clear()

            } else {

                status.text =
                    response.optString("error", "Login failed")
            }
        }
    }

    private fun getLocation(): Location? {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            status.text = "Please allow location permission."
            return null
        }

        val manager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        val candidates = mutableListOf<Location>()

        try {
            manager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
            )?.let {
                candidates.add(it)
            }

            manager.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER
            )?.let {
                candidates.add(it)
            }

        } catch (_: SecurityException) {
        }

        val best =
            candidates.maxByOrNull { it.time } ?: return null

        if (
            System.currentTimeMillis() - best.time >
            5 * 60 * 1000L
        ) {
            return null
        }

        return best
    }

    private fun mark(type: String) {

        val currentToken = token ?: run {
            status.text = "Please login first."
            return
        }

        val location = getLocation()

        if (location == null) {
            status.text =
                "Fresh GPS location unavailable. Turn on Location/GPS and try again."
            return
        }

        if (location.accuracy > 100f) {
            status.text =
                "GPS accuracy is weak (${location.accuracy.toInt()} m). Please wait for a better GPS signal."
            return
        }

        inButton.isEnabled = false
        outButton.isEnabled = false

        status.text = "Submitting $type attendance..."

        val deviceTime =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).format(Date())

        val body = JSONObject()
            .put("type", type)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("accuracy", location.accuracy)
            .put("device_time", deviceTime)

        request(
            "POST",
            "api/attendance.php",
            body,
            currentToken
        ) { response ->

            inButton.isEnabled = true
            outButton.isEnabled = true

            status.text =
                if (response.optBoolean("ok")) {

                    "${response.optString("message", "Attendance recorded")}\n" +
                    "Server time: ${response.optString("server_time")}\n" +
                    "GPS accuracy: ${location.accuracy.toInt()} m"

                } else {

                    response.optString(
                        "error",
                        "Attendance could not be recorded"
                    )
                }
        }
    }

    private fun loadHistory() {

        val currentToken = token ?: return

        historyButton.isEnabled = false
        status.text = "Loading attendance history..."

        request(
            "GET",
            "api/history.php",
            null,
            currentToken
        ) { response ->

            historyButton.isEnabled = true

            if (!response.optBoolean("ok")) {
                status.text =
                    response.optString(
                        "error",
                        "Could not load history"
                    )
                return@request
            }

            val records =
                response.optJSONArray("records") ?: JSONArray()

            val output =
                StringBuilder("LATEST ATTENDANCE\n\n")

            if (records.length() == 0) {
                output.append("No attendance records found.")
            }

            for (i in 0 until records.length()) {

                val item =
                    records.optJSONObject(i) ?: continue

                output.append(
                    item.optString("attendance_type")
                )

                output.append(" | ")
                    .append(item.optString("server_time"))

                output.append("\n")
                    .append(item.optString("branch"))

                output.append(" | GPS: ")
                    .append(item.optString("latitude"))

                output.append(", ")
                    .append(item.optString("longitude"))

                output.append("\n\n")
            }

            historyView.text = output.toString()

            status.text =
                "Showing latest ${records.length()} records."
        }
    }
}
