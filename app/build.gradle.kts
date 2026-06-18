plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.safety_monitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.safety_monitor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.activity:activity:1.9.3")
    implementation(libs.constraintlayout)

    // --- YOUR AI LIBRARIES (WITH FIXES) ---
    // Fix: Exclude JUnit/Hamcrest from JLibrosa so it doesn't crash the app
    implementation("com.litongjava:jlibrosa:1.1.8") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }

    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")

    // --- TEST DEPENDENCIES (WITH FIXES) ---
    // Fix: Exclude Hamcrest from standard JUnit to prevent duplicates
    testImplementation(libs.junit) {
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}