plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase is optional: drop your google-services.json into app/ to switch
// accounts from "this device only" to Firebase. Without it the app still builds.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.rotitrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rotitrack.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.6.0"
    }

    signingConfigs {
        // A committed debug key, so every CI test build can update the previous install.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // Also sign with the older v1 scheme: some Samsung installers reject v2-only
            // APKs as "package appears to be invalid".
            enableV1Signing = true
            enableV2Signing = true
        }
        // The Play Store upload key. CI writes it from the RELEASE_KEYSTORE_* repository
        // secrets (see .github/workflows/release.yml); it is never committed.
        val uploadKeystore = System.getenv("RELEASE_KEYSTORE_FILE")?.let(::file)
        if (uploadKeystore != null && uploadKeystore.exists()) {
            create("upload") {
                storeFile = uploadKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // The upload key when CI provides it; otherwise the debug key, so a local
            // release build still installs.
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.razorpay.checkout)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.billing.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
