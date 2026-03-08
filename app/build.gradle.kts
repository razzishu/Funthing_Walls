plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aesthetic.funthingwalls"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
        buildFeatures {
            viewBinding = true
        }
    }

    defaultConfig {
        applicationId = "com.aesthetic.funthingwalls"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Coil: Loads high-quality images without lagging the phone
    implementation("io.coil-kt:coil:2.4.0")

    // Retrofit: Connects to the Pexels "Huge Collection" API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // WorkManager: Runs the background timer for the Funthing Engine
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    // Testing tools (Required by Android Studio default files)
    implementation("androidx.palette:palette-ktx:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}