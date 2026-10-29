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
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val envPath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
            val propPath = (project.findProperty("KEYSTORE_PATH") as? String)?.takeIf { it.isNotBlank() }
            val customKeystorePath = envPath ?: propPath
            val projectKeystore = rootProject.file("keystore/echo-release.jks")

            if (customKeystorePath != null && file(customKeystorePath).isFile) {
                storeFile = file(customKeystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("KEYSTORE_PASSWORD") as? String) ?: "echomusicplayer"
                keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("KEY_ALIAS") as? String) ?: "echokey"
                keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("KEY_PASSWORD") as? String) ?: "echomusicplayer"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else if (projectKeystore.isFile) {
                storeFile = projectKeystore
                storePassword = "echomusicplayer"
                keyAlias = "echokey"
                keyPassword = "echomusicplayer"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else {
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
