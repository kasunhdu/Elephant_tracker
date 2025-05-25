plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Google Services plugin for Firebase
}

android {
    namespace = "com.example.my_elephant_collar"
    compileSdk = 35 // Your current compile SDK

    defaultConfig {
        applicationId = "com.example.my_elephant_collar"
        minSdk = 24 // Your current min SDK (API 24 or higher is good for getOrDefault)
        targetSdk = 35 // Your current target SDK
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true // Enable Jetpack Compose
    }

    composeOptions {
        // Set the Kotlin Compose compiler extension version.
        // This must be compatible with your Kotlin version and the Compose BOM.
        // For compose-bom:2025.05.01 and Kotlin 1.9.x, 1.5.12 is a common compatible version.
        kotlinCompilerExtensionVersion = "1.5.12"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Import the Compose BOM to manage Compose library versions.
    // This should be the *only* place you specify the Compose version for core Compose libraries.
    implementation(platform(libs.compose.bom))

    // Declare Compose dependencies without explicit versions.
    // Their versions will be managed by the 'compose-bom' imported above.
    // Remove any duplicate explicit version declarations for Compose UI, Material, Activity-Compose.
    implementation(libs.ui) // Assumes libs.ui points to androidx.compose.ui:ui
    implementation(libs.ui.tooling.preview) // Assumes libs.ui.tooling.preview points to androidx.compose.ui:ui-tooling-preview
    implementation(libs.material3) // Assumes libs.material3 points to androidx.compose.material3:material3
    implementation(libs.activity.compose) // Assumes libs.activity.compose points to androidx.activity:activity-compose


    // Core Android KTX and AppCompat - keep these if your app uses them
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    // Firebase platform and Realtime Database
    // Use the Firebase BOM to manage Firebase library versions.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx) // Explicitly include Realtime Database KTX

    // Google Maps Compose and Play Services Location
    implementation(libs.maps.compose) // Google Maps Compose library
    implementation(libs.play.services.maps) // Core Google Maps Play Services
    implementation(libs.play.services.location) // Assumes libs.play.services.location points to com.google.android.gms:play-services-location

    // Testing dependencies
    testImplementation(libs.junit.jupiter)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}