package com.spotify.clone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.EchoTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.ui.navigation.AppNavHost
import com.spotify.clone.ui.splash.EchoSplashOverlay

class MainActivity : ComponentActivity() {

    private var pendingIntent by mutableStateOf<Intent?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioPermGranted = results[Manifest.permission.READ_MEDIA_AUDIO] == true ||
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioPermGranted) {
            AudioRepository.notifyAudioPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splashProvider ->
            splashProvider.view.animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction { splashProvider.remove() }
                .start()
            splashProvider.iconView.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .alpha(0f)
                .setDuration(200L)
                .start()
        }

        ThemeManager.init(this)
        SettingsManager.init(this)
        UserProfileManager.init(this)
        pendingIntent = intent
        enableEdgeToEdge()
        requestAppPermissions()

        val hasDeepLink = intent?.data != null || intent?.hasExtra("nav_tab") == true

        setContent {
            EchoTheme {
                var showSplash by rememberSaveable { mutableStateOf(!hasDeepLink) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        pendingIntent = pendingIntent,
                        onIntentHandled = { pendingIntent = null }
                    )

                    EchoSplashOverlay(
                        visible = showSplash,
                        fastDismiss = hasDeepLink,
                        onFinished = { showSplash = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
