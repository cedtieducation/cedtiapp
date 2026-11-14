CEDTI STAFF ATTENDANCE - SOURCE PACKAGE v0.3.0 (PHOTO ATTENDANCE)

APP FEATURES
- Staff Code + password login
- Mark IN / OUT
- My attendance history
- Attendance requests go to https://cedtilearning.com/cedti-attendance/
- Mandatory camera photo captured before every IN/OUT attendance
- Photo uploaded with GPS and saved on the server
- Server-side GPS geofence: Chowari 70 metres (other branch radii remain as configured)
- Staff branch is taken from the server database; staff cannot choose another branch in the app

FIVE BRANCH PINS
Chowari: 32.4294489, 76.0115243
Sihunta: 32.3018609, 76.0877784
Chamba: 32.5673839, 76.1156214
Lanj: 32.0979075, 76.1455620
Suliyali: 32.3762260, 75.8894916

SERVER UPDATE (IMPORTANT)
1. Make a backup of the existing /cedti-attendance/ folder and database first.
2. Run server-update/sql/add-attendance-photo-column.sql once in the CEDTI attendance database (backup first).
3. Upload server-update/api/attendance.php and server-update/api/history.php into /cedti-attendance/api/ (overwrite those two files only).
4. Upload server-update/branch_locations.php into /cedti-attendance/branch_locations.php. It sets Chowari to 70 metres and leaves the other branches at their values in this source package.
5. Do NOT overwrite config.php, db.php, admin files, or existing database data.
6. Confirm that /cedti-attendance/api/ can reach the existing ../db.php and that api_tokens exists.
7. Create /cedti-attendance/uploads/attendance/ if needed and ensure PHP can write to it (typically folder permission 0750 or hosting-panel equivalent).
8. Test one staff login, then test attendance while standing at the correct branch.

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


PHOTO FEATURE NOTES
- The Android app opens the phone's camera before each IN/OUT. Attendance is not sent if the user cancels the photo.
- The camera app is asked to use the front camera, but some phone camera apps may ignore that hint.
- The returned camera thumbnail is JPEG-compressed and uploaded with the attendance request.
- The API saves the JPEG under uploads/attendance/YYYY/MM and stores its relative path in attendance.photo_path.
- This ZIP does not include the existing admin/ folder or report.php. The photo is stored in the database/server, but the Admin Panel's photo thumbnail/view column still needs to be added to your actual admin report.php. Upload that file separately if you want me to integrate it precisely.
- This package is source code. Build a new APK with GitHub Actions after committing the updated android/ folder.
