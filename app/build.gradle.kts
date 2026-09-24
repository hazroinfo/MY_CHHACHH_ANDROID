plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mychhachh.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mychhachh.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "3.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {}
