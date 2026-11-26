package com.spotify.clone.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String): String =
            "playlist/${Uri.encode(playlistId)}"
    }
    object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String): String =
            "artist/${Uri.encode(artistName)}"
    }
}
