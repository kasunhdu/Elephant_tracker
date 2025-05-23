plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.my_elephant_collar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.my_elephant_collar"
        minSdk = 21
        targetSdk = 35
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
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
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    testImplementation(libs.junit.jupiter)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    // Firebase platform and Realtime Database
    implementation(platform("com.google.firebase:firebase-bom:32.8.1")) // Use the latest BOM version
    implementation("com.google.firebase:firebase-database-ktx")

    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:4.3.0") // Use the latest version
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Use the latest version
}

