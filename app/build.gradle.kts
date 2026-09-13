plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.amoledgallery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.amoledgallery"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI & Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil 3 for Compose, Video Frame Decoder, and GIF decoder
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-video:3.0.4")
    implementation("io.coil-kt.coil3:coil-gif:3.0.4")

    // Real backdrop blur for the glass bars, sheets and dialogs (iOS Liquid Glass, One UI and
    // HyperOS frosted surfaces). 1.1.x is the last line built against Compose 1.7 / Kotlin 2.0.
    implementation("dev.chrisbanes.haze:haze-android:1.1.1")

    // Media3 ExoPlayer for native video playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // Media3 Transformer for exporting cropped / rotated / mirrored videos in the editor
    implementation("androidx.media3:media3-transformer:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")

    // ExifInterface for reading image camera metadata
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Installs the baseline profile bundled in src/main/baseline-prof.txt so ART AOT-compiles the
    // scroll/startup hot paths. Without it a sideloaded APK installs with compiler filter "verify"
    // and runs fully interpreted - which was measured to be the dominant cause of scroll jank.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
