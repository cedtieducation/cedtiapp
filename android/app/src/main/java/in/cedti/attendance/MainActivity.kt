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

    private fun addButton(text: String, action: () -> Unit) {
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
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        mainLayout.addView(input)
        return input
    }

    // ---------------- LOGIN SCREEN ----------------

    private fun showLogin() {
        setupLayout()

        addTitle("CEDTI STAFF ATTENDANCE")
        addText("Staff Login")

        loginCode = addInput("Enter Staff Code")
        loginPassword = addInput("Enter Password", true)

        statusText = TextView(this)
        statusText.textSize = 14f
        statusText.setTextColor(Color.RED)
        mainLayout.addView(statusText)

        addButton("LOGIN") {

            val code = loginCode.text.toString().trim()
            val password = loginPassword.text.toString()

            if (code.isEmpty() || password.isEmpty()) {
                statusText.text =
                    "Please enter Staff Code and Password"
            } else {
                login(code, password)
            }
        }

        addText(
            "CEDTI Institute of Management & Technology",
            13f
        )
    }

    // ---------------- LOGIN API ----------------

    private fun login(code: String, password: String) {

        statusText.text = "Connecting to server..."

        thread {
            try {

                val data = JSONObject()
                data.put("staff_code", code)
                data.put("password", password)

                val response = postRequest(
                    "api/login.php",
                    data
                )

                runOnUiThread {

                    if (response.optBoolean("ok")) {

                        val staff =
                            response.optJSONObject("staff")
                                ?: JSONObject()

                        staffCode = staff.optString(
                            "staff_code",
                            code
                        )

                        staffName = staff.optString(
                            "name",
                            "Staff"
                        )

                        authToken = response.optString(
                            "token",
                            ""
                        )

                        if (authToken.isEmpty()) {
                            statusText.text =
                                "Login token missing from server"
                            return@runOnUiThread
                        }

                        getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE
                        ).edit()
                            .putString("staff_code", staffCode)
                            .putString("staff_name", staffName)
                            .putString("auth_token", authToken)
                            .putString(
                                "branch",
                                staff.optString("branch", "")
                            )
                            .apply()

                        showDashboard()

                    } else {

                        statusText.text =
                            "LOGIN FAILED\n" +
                            "HTTP Code: ${response.optInt("_http_code", 0)}\n" +
                            "Server Response: ${response}"
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {
                    statusText.text =
                        "CONNECTION ERROR:\n${e.javaClass.simpleName}: ${e.message}"
                }
            }
        }
    }

    // ---------------- DASHBOARD ----------------

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
            authToken = ""

            showLogin()
        }

        addText(
            "CEDTI Institute of Management & Technology",
            13f
        )
    }

    // ---------------- ATTENDANCE ----------------

    private fun markAttendance(type: String) {

        if (!hasLocationPermission()) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )

            statusText.text =
                "Please allow location permission"

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
                        response.optString(
                            "error",
                            if (response.optBoolean("ok"))
                                "Attendance submitted"
                            else
                                "Attendance failed"
                        )
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {
                    statusText.text =
                        "Error: ${e.message}"
                }
            }
        }
    }

    // ---------------- ATTENDANCE HISTORY ----------------

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

                    if (!response.optBoolean("ok") &&
                        !response.optBoolean("success")) {

                        addText(
                            response.optString(
                                "error",
                                response.optString(
                                    "message",
                                    "Unable to load history"
                                )
                            )
                        )

                        return@runOnUiThread
                    }

                    val records =
                        response.optJSONArray("records")
                            ?: response.optJSONArray("data")
                            ?: JSONArray()

                    if (records.length() == 0) {
                        addText("No attendance records found.")
                    }

                    for (i in 0 until records.length()) {

                        val item =
                            records.optJSONObject(i) ?: continue

                        val date = item.optString(
                            "server_time",
                            item.optString("date", "")
                        )

                        val type = item.optString("type", "")
                        val latitude =
                            item.optString("latitude", "")

                        val longitude =
                            item.optString("longitude", "")

                        addText(
                            "Date: $date\n" +
                            "Attendance: $type\n" +
                            "Latitude: $latitude\n" +
                            "Longitude: $longitude\n" +
                            "--------------------------"
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {
                    statusText.text =
                        "Error: ${e.message}"
                }
            }
        }
    }

    // ---------------- LOCATION PERMISSION ----------------

    private fun hasLocationPermission(): Boolean {

        return checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||

        checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ---------------- GET LOCATION ----------------

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

    // ---------------- POST API REQUEST ----------------

    private fun postRequest(
        endpoint: String,
        data: JSONObject
    ): JSONObject {

        val url = URL(baseUrl + endpoint)

        val connection =
            url.openConnection() as HttpURLConnection

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

        if (authToken.isNotEmpty() &&
            endpoint != "api/login.php") {

            connection.setRequestProperty(
                "Authorization",
                "Bearer $authToken"
            )
        }

        connection.outputStream.use {
            it.write(
                data.toString().toByteArray(Charsets.UTF_8)
            )
        }

        val responseCode = connection.responseCode

        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseText = stream?.bufferedReader()?.use {
            it.readText()
        } ?: "{}"

        connection.disconnect()

        return try {

            JSONObject(responseText).put(
                "_http_code",
                responseCode
            )

        } catch (e: Exception) {

            JSONObject()
                .put("_http_code", responseCode)
                .put(
                    "error",
                    "Invalid server response: $responseText"
                )
        }
    }

    // ---------------- PERMISSION RESULT ----------------

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
```
