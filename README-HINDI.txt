CEDTI STAFF ATTENDANCE - SOURCE PACKAGE v0.2.0

APP FEATURES
- Staff Code + password login
- Mark IN / OUT
- My attendance history
- Attendance requests go to https://cedtilearning.com/cedti-attendance/
- Server-side GPS geofence: 20 metres around the assigned branch
- Staff branch is taken from the server database; staff cannot choose another branch in the app

FIVE BRANCH PINS
Chowari: 32.4294489, 76.0115243
Sihunta: 32.3018609, 76.0877784
Chamba: 32.5673839, 76.1156214
Lanj: 32.0979075, 76.1455620
Suliyali: 32.3762260, 75.8894916

SERVER UPDATE (IMPORTANT)
1. Make a backup of the existing /cedti-attendance/ folder and database first.
2. Upload the three files from server-update/api/ into /cedti-attendance/api/ (overwrite only login.php, attendance.php, history.php).
3. Upload server-update/branch_locations.php into /cedti-attendance/branch_locations.php.
4. Do NOT overwrite config.php, db.php, admin files, or the database.
5. Confirm that /cedti-attendance/api/ can reach the existing ../db.php and that api_tokens exists.
6. Test one staff login, then test attendance while standing at the correct branch.

The API no longer expects an embedded API secret in the APK. Staff login and expiring bearer tokens protect attendance/history endpoints. Keep HTTPS enabled.

BUILD APK
This folder is the Android Studio/Gradle source. The server-update subfolder is for the website. To build on GitHub without setting up Android Studio:
1. Upload the contents of this cedtiapp folder to a new GitHub repository (folder contents, not the outer ZIP folder).
2. Open the repository's Actions tab.
3. Select 'Build CEDTI Attendance APK' and press 'Run workflow'.
4. When completed, open the run and download the artifact named CEDTI-Staff-Attendance-APK.
5. Extract app-debug.apk and share it with staff only after testing.

IMPORTANT LIMITATIONS
- This version implements GPS geofencing, not face recognition or liveness detection. A staff photo in the admin panel is not a face match.
- Lanj pin coordinates should be checked against the actual centre entrance before production use. A 20 m geofence can reject genuine attendance if a pin is misplaced or phone GPS is inaccurate.
- Android source was updated, but an APK has NOT been compiled in this workspace because Android SDK/Gradle are not installed here.
