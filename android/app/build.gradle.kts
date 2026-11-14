plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "in.cedti.attendance"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.cedti.attendance"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "BASE_URL", "\"https://cedtilearning.com/cedti-attendance/\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
