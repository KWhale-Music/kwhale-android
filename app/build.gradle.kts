import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val appsDebugKeystore = providers.gradleProperty("APPS_DEBUG_KEYSTORE").orNull
val appsKeystore = providers.gradleProperty("APPS_KEYSTORE").orNull
val appsStorePassword = providers.gradleProperty("APPS_STORE_PASSWORD").orNull
val appsKeyAlias = providers.gradleProperty("APPS_KEY_ALIAS").orNull
val appsKeyPassword = providers.gradleProperty("APPS_KEY_PASSWORD").orNull

// Dev server URL override (gitignored `local.properties`):
//   API_BASE_URL=http://<your-lan-host>:19001/v1
// Defaults to the Android emulator loopback of the self-hosted KWhale API.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val devApiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:19001/v1")

android {
    namespace = "org.dueattendant149.kwhale"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.dueattendant149.kwhale"
        minSdk = 31
        targetSdk = 35
        versionCode = 32
        versionName = "0.1.22"
        buildConfigField("String", "API_BASE_URL", "\"https://music.dueattendant149.org/v1\"")
    }

    signingConfigs {
        if (appsDebugKeystore != null) {
            create("sharedDebug") {
                storeFile = File(appsDebugKeystore)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (appsKeystore != null && appsStorePassword != null && appsKeyAlias != null && appsKeyPassword != null) {
            create("release") {
                storeFile = File(appsKeystore)
                storePassword = appsStorePassword
                keyAlias = appsKeyAlias
                keyPassword = appsKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("sharedDebug") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            // Dev server: override via API_BASE_URL in (gitignored) local.properties;
            // default is the emulator loopback of the self-hosted KWhale API.
            // Cleartext is allowed for debug builds (see src/debug network config).
            buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Optional: without APPS_* in ~/.gradle/gradle.properties (or CI secrets)
            // the release APK is built unsigned; debug/lint/unit tests still work.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:design"))
    implementation(project(":core:player"))
    implementation(project(":core:network"))

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
