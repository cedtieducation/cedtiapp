package `in`.cedti.attendance

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.*
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.Bitmap
import android.content.Intent
import android.util.Base64
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
    private var pendingAttendanceType: String? = null

    private lateinit var status: TextView
    private lateinit var staffCode: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var inButton: Button
    private lateinit var outButton: Button
    private lateinit var historyButton: Button
    private lateinit var historyView: TextView
    private lateinit var form: LinearLayout

    private val CAMERA_REQUEST = 201

    private val baseUrl =
        BuildConfig.BASE_URL.trimEnd('/') + "/"

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
            setOnClickListener {
                doLogin()
            }
        }

        inButton = Button(this).apply {
            text = "MARK IN"
            isEnabled = false
            setOnClickListener {
                mark("IN")
            }
        }

        outButton = Button(this).apply {
            text = "MARK OUT"
            isEnabled = false
            setOnClickListener {
                mark("OUT")
            }
        }

        historyButton = Button(this).apply {
            text = "MY ATTENDANCE HISTORY"
            isEnabled = false
            setOnClickListener {
                loadHistory()
            }
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

                password.text.clear()
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
                    stream?.bufferedReader()?.use {
                        it.readText()
                    } ?: "{}"

                val result = try {
                    JSONObject(responseText)
                } catch (_: Exception) {
                    JSONObject()
                        .put("ok", false)
                        .put("error", responseText.take(500))
                }

                result.put("_http_code", code)

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

        request(
            "POST",
            "api/login.php",
            body
        ) { response ->

            loginButton.isEnabled = true

            val httpCode = response.optInt("_http_code", 0)

            if (response.optBoolean("ok")) {

                token = response.optString("token")

                val staff =
                    response.optJSONObject("staff") ?: JSONObject()

                val name = staff.optString("name")
                val branch = staff.optString("branch")

                status.text =
                    "Welcome $name\nBranch: $branch"

                inButton.isEnabled = true
                outButton.isEnabled = true
                historyButton.isEnabled = true

                staffCode.isEnabled = false
                password.isEnabled = false

                password.text.clear()

            } else {

                val error =
                    response.optString("error", "Login failed")

                status.text =
                    "LOGIN FAILED\nHTTP Code: $httpCode\n$error"
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

        if (System.currentTimeMillis() - best.time > 5 * 60 * 1000L) {
            return null
        }

        return best
    }

    // Attendance requires a fresh camera photo before the GPS attendance request.
    private fun mark(type: String) {
        if (token == null) {
            status.text = "Please login first."
            return
        }

        pendingAttendanceType = type
        try {
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                // Hint to camera apps to use the front camera where supported.
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            // Do not call resolveActivity(): Android 11+ package visibility can
            // hide camera apps unless queried. Launch the camera intent directly.
            status.text = "Opening camera... Please take your attendance photo."
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        } catch (e: Exception) {
            pendingAttendanceType = null
            status.text = "Could not open camera: ${e.localizedMessage}"
        }
    }

    @Deprecated("Deprecated by Android, retained for broad camera-app compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAMERA_REQUEST) return

        val type = pendingAttendanceType
        pendingAttendanceType = null

        if (resultCode != RESULT_OK || data == null) {
            status.text = "Photo was not captured. Attendance was not submitted."
            return
        }

        val photo = data.extras?.get("data") as? Bitmap
        if (photo == null) {
            status.text = "Camera did not return a photo. Please try again."
            return
        }

        val output = java.io.ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.JPEG, 78, output)
        val encodedPhoto = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        if (encodedPhoto.length > 4_000_000) {
            status.text = "Photo is too large. Please retake the photo."
            return
        }

        if (type != null) submitAttendance(type, encodedPhoto)
    }

    private fun submitAttendance(type: String, photoBase64: String) {
        val currentToken = token ?: run {
            status.text = "Please login first."
            return
        }

        val location = getLocation()
        if (location == null) {
            status.text = "Fresh GPS location unavailable. Turn on Location/GPS and try again. Photo was not submitted."
            return
        }

        if (location.accuracy > 100f) {
            status.text = "GPS accuracy is weak (${location.accuracy.toInt()} m). Please wait for a better GPS signal. Photo was not submitted."
            return
        }

        inButton.isEnabled = false
        outButton.isEnabled = false
        status.text = "Uploading photo and submitting $type attendance..."

        val deviceTime = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        ).format(Date())

        val body = JSONObject()
            .put("type", type)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("accuracy", location.accuracy)
            .put("device_time", deviceTime)
            .put("photo_base64", photoBase64)

        request("POST", "api/attendance.php", body, currentToken) { response ->
            inButton.isEnabled = true
            outButton.isEnabled = true

            if (response.optBoolean("ok")) {
                status.text =
                    "${response.optString("message", "Attendance recorded")}\n" +
                    "Server time: ${response.optString("server_time")}\n" +
                    "GPS accuracy: ${location.accuracy.toInt()} m\n" +
                    "Photo: uploaded successfully"
            } else {
                status.text =
                    "Attendance failed (HTTP ${response.optInt("_http_code")})\n" +
                    response.optString("error", "Attendance could not be recorded")
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
                    "History failed (HTTP ${response.optInt("_http_code")})\n" +
                    response.optString("error", "Could not load history")

                return@request
            }

            val records: JSONArray =
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

                output.append(
                    item.optString("server_time")
                )

                output.append("\n")

                output.append(
                    item.optString("branch")
                )

                output.append(" | GPS: ")

                output.append(
                    item.optString("latitude")
                )

                output.append(", ")

                output.append(
                    item.optString("longitude")
                )

                output.append("\n\n")
            }

            historyView.text = output.toString()

            status.text =
                "Showing latest ${records.length()} records."
        }
    }
}
