import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.spotify.clone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.echo.musicplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "2.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localPropsFile = rootProject.file("local.properties")
        val localProps = Properties().apply {
            if (localPropsFile.isFile) {
                localPropsFile.inputStream().use { load(it) }
            }
        }
        val spotifyClientId = (System.getenv("SPOTIFY_CLIENT_ID")
            ?: localProps.getProperty("SPOTIFY_CLIENT_ID", ""))
            .replace("\"", "")
        val spotifyClientSecret = (System.getenv("SPOTIFY_CLIENT_SECRET")
            ?: localProps.getProperty("SPOTIFY_CLIENT_SECRET", ""))
            .replace("\"", "")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$spotifyClientSecret\"")
    }

    signingConfigs {
        create("release") {
            // Signing secrets come only from env vars, Gradle properties, or the
            // git-ignored local.properties. Never hardcode them here.
            val signingProps = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.isFile) f.inputStream().use { load(it) }
            }
            fun signingValue(name: String): String? =
                (System.getenv(name)
                    ?: (project.findProperty(name) as? String)
                    ?: signingProps.getProperty(name))
                    ?.takeIf { it.isNotBlank() }

            val releaseKeystore = (signingValue("KEYSTORE_PATH")?.let { rootProject.file(it) }
                ?: rootProject.file("keystore/echo-release.jks"))
                .takeIf { it.isFile }
            val releaseStorePassword = signingValue("KEYSTORE_PASSWORD")
            val releaseKeyAlias = signingValue("KEY_ALIAS")
            val releaseKeyPassword = signingValue("KEY_PASSWORD") ?: releaseStorePassword

            if (releaseKeystore != null && releaseStorePassword != null && releaseKeyAlias != null) {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else {
                logger.warn(
                    "ECHO: release keystore or KEYSTORE_PASSWORD/KEY_ALIAS not set; " +
                        "release build will be signed with the DEBUG key."
                )
                val debugConfig = getByName("debug")
                var debugStore = debugConfig.storeFile
                if (debugStore == null || !debugStore.isFile) {
                    val fallbackKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    if (!fallbackKeystore.isFile) {
                        fallbackKeystore.parentFile?.mkdirs()
                        try {
                            val keytool = org.gradle.internal.jvm.Jvm.current().javaHome.resolve("bin/keytool").absolutePath
                            ProcessBuilder(
                                keytool, "-genkey", "-v",
                                "-keystore", fallbackKeystore.absolutePath,
                                "-alias", "androiddebugkey",
                                "-storepass", "android",
                                "-keypass", "android",
                                "-keyalg", "RSA",
                                "-keysize", "2048",
                                "-validity", "10000",
                                "-dname", "CN=Android Debug,O=Android,C=US"
                            ).inheritIO().start().waitFor()
                        } catch (_: Exception) {}
                    }
                    debugStore = fallbackKeystore
                }
                storeFile = debugStore
                storePassword = debugConfig.storePassword ?: "android"
                keyAlias = debugConfig.keyAlias ?: "androiddebugkey"
                keyPassword = debugConfig.keyPassword ?: "android"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        htmlReport = true
        xmlReport = true
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Media3 & ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Image Loading
    implementation(libs.coil.compose)

    // AndroidX Core Splashscreen
    implementation(libs.androidx.core.splashscreen)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // Android Instrumentation & UI Testing
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
