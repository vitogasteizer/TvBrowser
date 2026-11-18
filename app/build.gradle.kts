plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val repo = System.getenv("GITHUB_REPOSITORY") ?: ""
val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2

android {
    namespace = "com.example.tvbrowser"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.tvbrowser"
        minSdk = 22
        targetSdk = 34
        versionCode = run + 100
        versionName = "2.$run"
        val updateUrl = if (repo.isEmpty()) "" else "https://github.com/$repo/releases/download/latest/app-debug.apk"
        buildConfigField("String", "UPDATE_URL", "\"$updateUrl\"")
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        create("tvb") {
            storeFile = file("tvb.keystore")
            storePassword = "tvbrowser"
            keyAlias = "tvb"
            keyPassword = "tvbrowser"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("tvb") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.core:core:1.12.0")
}
