plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

// ... rest of your android {} and dependencies {} blocks remain exactly as they are

android {
    namespace = "com.example.safety_monitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.safety_monitor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        // Fix for TensorFlow license files
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "META-INF/DEPENDENCIES"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Changed to 1.8 (Standard for Android)
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        mlModelBinding = false
    }
}

dependencies {
//    implementation("androidx.navigation:navigation-compose:2.7.7")
//    implementation("androidx.compose.material:material-icons-extended:1.6.3")
//    implementation(libs.appcompat)
//    implementation(libs.material)
//    implementation("androidx.activity:activity:1.9.3")
//    implementation(libs.constraintlayout)
//
//    // --- YOUR AI LIBRARIES (WITH FIXES) ---
//    // Fix: Exclude JUnit/Hamcrest from JLibrosa so it doesn't crash the app
//    implementation("com.litongjava:jlibrosa:1.1.8") {
//        exclude(group = "junit", module = "junit")
//        exclude(group = "org.hamcrest", module = "hamcrest-core")
//    }
//
//    implementation("org.tensorflow:tensorflow-lite:2.17.0")
//    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
//
//    // --- TEST DEPENDENCIES (WITH FIXES) ---
//    // Fix: Exclude Hamcrest from standard JUnit to prevent duplicates
//    testImplementation(libs.junit) {
//        exclude(group = "org.hamcrest", module = "hamcrest-core")
//    }
//
//    androidTestImplementation(libs.ext.junit)
//    androidTestImplementation(libs.espresso.core)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase Platform BOM & Auth
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")

    // Android Credential Manager for Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0-alpha04")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha04")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Google Play Services for Location Tracking
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // TensorFlow Lite Audio Task Processing
    implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")

    // Testing Support Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}