CEDTI Android App Icon Assets

Copy the files into:
android/app/src/main/res/

This ZIP contains:
mipmap-mdpi/ic_launcher.png and ic_launcher_round.png
mipmap-hdpi/ic_launcher.png and ic_launcher_round.png
mipmap-xhdpi/ic_launcher.png and ic_launcher_round.png
mipmap-xxhdpi/ic_launcher.png and ic_launcher_round.png
mipmap-xxxhdpi/ic_launcher.png and ic_launcher_round.png

Then set these attributes on the <application> element in AndroidManifest.xml:
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"

Do not put these files in drawable. Keep the folder names exactly as shown.
