plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mychhachh.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mychhachh.app.noline"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "2.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {}
