package com.spotify.clone.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spotify.clone.core.player.MusicController

/**
 * Dedicated receiver for widget interactive controls.
 * Ensures a single authoritative recipient for playback actions, avoiding
 * duplicate toggle triggers when multiple widget providers are installed.
 */
class EchoWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received widget action: $action")

        val musicController = MusicController.getInstance(context)
        when (action) {
            EchoWidgetManager.ACTION_PLAY_PAUSE -> {
                musicController.togglePlayPause()
            }
            EchoWidgetManager.ACTION_NEXT -> {
                musicController.skipNext()
            }
            EchoWidgetManager.ACTION_PREV -> {
                musicController.skipPrevious()
            }
        }
    }

    companion object {
        private const val TAG = "EchoWidgetActionReceiver"
    }
}
