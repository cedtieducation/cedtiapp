CEDTI STAFF ATTENDANCE - ANDROID APP SOURCE (v0.2.0)
====================================================

This package contains the Android Studio source and two PHP API files required by this app.
It deliberately does NOT contain config.php, database passwords, API secrets, or existing student/staff data.

WHAT IS INCLUDED
- Android app: staff code/password login, GPS attendance IN/OUT, latest 50 attendance records, logout.
- api/login.php: authenticates a staff member using the existing staff.password_hash.
- api/history.php: returns the logged-in staff member's own attendance history.

IMPORTANT SERVER STEPS
1. Back up your current cedti-attendance folder first.
2. Upload the included api/login.php to:
   public_html/cedti-attendance/api/login.php
   (replace the existing file after backup).
3. Upload api/history.php to:
   public_html/cedti-attendance/api/history.php
4. Keep your existing api/bootstrap.php and config.php. Do not overwrite config.php.
5. Confirm this URL opens and returns JSON:
   https://cedtilearning.com/cedti-attendance/api/health.php
6. Confirm api_tokens table exists in the existing database. Do not drop tables or delete attendance data.

ANDROID APK BUILD
1. Install Android Studio on a Windows/Mac/Linux computer.
2. Extract this ZIP.
3. Android Studio > Open > select the "android" folder.
4. Wait for Gradle Sync to finish. Internet is needed on first build to download Android Gradle dependencies.
5. Build > Build Bundle(s) / APK(s) > Build APK(s).
6. APK output is normally:
   android/app/build/outputs/apk/debug/app-debug.apk
7. Install the debug APK on a test phone and test with a test staff account first.

SERVER ADDRESS
The Android app is configured for:
https://cedtilearning.com/cedti-attendance/

STAFF ACCOUNT
A staff record must already exist in the staff table, be active=1, and have a password_hash generated with PHP password_hash().
Do not use the Admin password as a staff password.

GPS NOTES
- The app reads the device's last known GPS/network location and rejects locations older than 5 minutes or accuracy worse than 100 metres.
- Enable Location on the phone and grant location permission.
- GPS coordinates and accuracy are stored with server time.
- This is GPS capture only. No branch geofence has been configured because approved coordinates for each branch and its permitted radius have not been supplied.
- GPS can be spoofed on compromised devices; this is not tamper-proof attendance.

FACE FEATURE
Face registration, biometric face matching, and liveness/anti-spoof verification are NOT included in this app version. The current app does not claim to verify a person's face.

SECURITY
- Login API is intentionally not protected by a static API key embedded in the APK. A static key inside an APK can be extracted by anyone who receives it.
- Keep HTTPS enabled. Use strong, unique staff passwords and restrict/monitor failed login attempts at server level before public rollout.
- Never put config.php, database credentials, or API secrets inside the Android project or a distributable ZIP.
- Test with one dummy staff account before giving the APK to staff.

BUILD LIMITATION
This deliverable is Android Studio source code, not a compiled APK. A compiled APK requires Android SDK and Gradle dependencies, which must be installed on a build computer.
