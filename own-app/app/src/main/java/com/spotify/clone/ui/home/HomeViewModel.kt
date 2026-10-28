package com.spotify.clone.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.mvi.UiEffect
import com.spotify.clone.core.mvi.UiEvent
import com.spotify.clone.core.mvi.UiState
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.recommendation.TeluguMusicRecommender
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class HomeState(
    val isLoading: Boolean = true,
    val selectedMood: String = "All",
    val selectedFilter: String = "All",
    val selectedLanguage: String = "All",
    val currentHero: EditorialBanner? = null,
    val editorialBanners: List<EditorialBanner> = emptyList(),
    val sculpturalAlbums: List<Playlist> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val madeForYou: List<Playlist> = emptyList(),
    val quickPlayGrid: List<Playlist> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList(),
    val bestNewSongs: List<Track> = emptyList(),
    val newAlbums: List<Playlist> = emptyList(),
    val dailyTop100: List<Track> = emptyList(),
    val jioSaavnCharts: List<Playlist> = emptyList(),
    val jioSaavnTrending: List<Playlist> = emptyList(),
    val artistRadios: List<Artist> = emptyList(),
    val popularArtists: List<Artist> = emptyList(),
    val recommendedTracks: List<Track> = emptyList(),
    val podcasts: List<Playlist> = emptyList(),
    val podcastEpisodes: List<Track> = emptyList()
) : UiState

sealed interface HomeEvent : UiEvent {
    data class SelectMood(val mood: String) : HomeEvent
    data class SelectFilter(val filter: String) : HomeEvent
    data class SelectLanguage(val language: String) : HomeEvent
    data class StartArtistRadio(val artist: Artist) : HomeEvent
    data class PlayPlaylist(val playlist: Playlist) : HomeEvent
    data class PlayTrack(val track: Track, val queue: List<Track>) : HomeEvent
    data class PlayHero(val banner: EditorialBanner) : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data class NavigateToPlaylist(val playlistId: String) : HomeEffect
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private val musicController = MusicController.getInstance(application)
    private val recommender = TeluguMusicRecommender.getInstance(application)

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    private var allPlaylistsCache: List<Playlist> = emptyList()
    private var allTracksCache: List<Track> = emptyList()
    private var allBannersCache: List<EditorialBanner> = emptyList()

    companion object {
        @Volatile
        private var cachedHomeState: HomeState? = null
    }

    init {
        val cached = cachedHomeState
        if (cached != null) {
            _uiState.value = cached.copy(isLoading = false)
            allPlaylistsCache = cached.featuredPlaylists
            allTracksCache = cached.recommendedTracks
            allBannersCache = cached.editorialBanners
            loadHomeData(isBackgroundRefresh = true)
        } else {
            loadHomeData(isBackgroundRefresh = false)
        }
    }

    private fun loadHomeData(isBackgroundRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isBackgroundRefresh && _uiState.value.quickPlayGrid.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val banners = repository.getEditorialBanners()
            allBannersCache = banners
            val podcasts = repository.getPodcasts()
            val podcastEpisodes = repository.getPodcastEpisodes()

            // STAGE 1: Fast Critical Content (Playlists & Artists with 2.5s timeout fallback)
            val playlistsDeferred = async {
                try {
                    withTimeoutOrNull(2500) { repository.getHomePlaylists() } ?: SampleCatalog.samplePlaylists
                } catch (e: Exception) {
                    SampleCatalog.samplePlaylists
                }
            }
            val artistsDeferred = async {
                try {
                    withTimeoutOrNull(2500) { repository.getHomeArtists() } ?: SampleCatalog.sampleArtists
                } catch (e: Exception) {
                    SampleCatalog.sampleArtists
                }
            }

            val playlists = playlistsDeferred.await()
            val artists = artistsDeferred.await()

            allPlaylistsCache = playlists
            val recommendedHero = recommender.getRecommendedHero("All")
            val sculptural = playlists.filter { it.id != "pl_liked_songs" }
            val madeForYouList = playlists.filter { it.id != "pl_liked_songs" }

            // Immediately unblock UI and dismiss Skeleton in under 400ms!
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    currentHero = recommendedHero,
                    editorialBanners = banners,
                    sculpturalAlbums = if (current.sculpturalAlbums.isNotEmpty()) current.sculpturalAlbums else sculptural,
                    madeForYou = if (current.madeForYou.isNotEmpty()) current.madeForYou else madeForYouList,
                    recentlyPlayed = emptyList(),
                    quickPlayGrid = playlists.take(8),
                    featuredPlaylists = playlists,
                    artistRadios = if (current.artistRadios.isNotEmpty()) current.artistRadios else artists,
                    popularArtists = if (current.popularArtists.isNotEmpty()) current.popularArtists else artists,
                    podcasts = podcasts,
                    podcastEpisodes = podcastEpisodes
                )
            }

            // STAGE 2: Enrichment in background (Trending, Top 100, New Albums, JioSaavn Charts)
            launch {
                val bestNewSongsDeferred = async { try { withTimeoutOrNull(3000) { repository.getBestNewSongs() } ?: emptyList() } catch (e: Exception) { emptyList() } }
                val newAlbumsDeferred = async { try { withTimeoutOrNull(3000) { repository.getNewAlbums() } ?: emptyList() } catch (e: Exception) { emptyList() } }
                val dailyTop100Deferred = async { try { withTimeoutOrNull(3000) { repository.getDailyTop100() } ?: emptyList() } catch (e: Exception) { emptyList() } }
                val saavnChartsDeferred = async { try { withTimeoutOrNull(3000) { repository.getJioSaavnCharts() } ?: emptyList() } catch (e: Exception) { emptyList() } }
                val saavnTrendingDeferred = async { try { withTimeoutOrNull(3000) { repository.getJioSaavnTrending() } ?: emptyList() } catch (e: Exception) { emptyList() } }
                val saavnRadiosDeferred = async { try { withTimeoutOrNull(3000) { repository.getJioSaavnArtistRadios() } ?: emptyList() } catch (e: Exception) { emptyList() } }

                val bestNewSongs = bestNewSongsDeferred.await()
                val newAlbums = newAlbumsDeferred.await()
                val dailyTop100 = dailyTop100Deferred.await()
                val saavnCharts = saavnChartsDeferred.await()
                val saavnTrending = saavnTrendingDeferred.await()
                val saavnRadios = saavnRadiosDeferred.await()

                val topTracks = if (dailyTop100.isNotEmpty()) dailyTop100 else if (bestNewSongs.isNotEmpty()) bestNewSongs else SampleCatalog.sampleTracks
                allTracksCache = topTracks

                val curatedArtists = buildList {
                    if (saavnRadios.isNotEmpty()) addAll(saavnRadios)
                    addAll(artists)
                }.distinctBy { it.name.lowercase().trim() }

                val updatedSculptural = if (newAlbums.isNotEmpty()) newAlbums else sculptural
                val updatedMadeForYou = if (saavnTrending.isNotEmpty()) saavnTrending else madeForYouList

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        sculpturalAlbums = updatedSculptural,
                        madeForYou = updatedMadeForYou,
                        bestNewSongs = topTracks.take(15),
                        newAlbums = updatedSculptural,
                        dailyTop100 = topTracks,
                        jioSaavnCharts = saavnCharts,
                        jioSaavnTrending = saavnTrending,
                        artistRadios = curatedArtists,
                        popularArtists = curatedArtists,
                        recommendedTracks = topTracks
                    )
                }
                cachedHomeState = _uiState.value
            }
        }
    }

    fun dispatch(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectMood -> {
                val mood = event.mood
                _uiState.update { current ->
                    // Adapt the hero card, albums, and mixes based on selected mood using the Telugu recommender
                    val moodHero = recommender.getRecommendedHero(mood)
                    val filteredSculptural = recommender.getRecommendedTeluguAlbums(mood, allPlaylistsCache)
                    val filteredMadeForYou = recommender.getMadeForYouPacks(mood)
                    val rankedTracks = recommender.rankTracksForRecommendation(allTracksCache, mood)

                    current.copy(
                        selectedMood = mood,
                        currentHero = moodHero,
                        sculpturalAlbums = filteredSculptural,
                        madeForYou = filteredMadeForYou,
                        bestNewSongs = rankedTracks.take(15),
                        recommendedTracks = rankedTracks
                    )
                }
            }

            is HomeEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }

            is HomeEvent.SelectLanguage -> {
                _uiState.update { it.copy(selectedLanguage = event.language) }
                viewModelScope.launch {
                    val langTracks = repository.getSongsForLanguage(event.language)
                    if (langTracks.isNotEmpty()) {
                        _uiState.update { it.copy(recommendedTracks = langTracks) }
                    }
                }
            }

            is HomeEvent.StartArtistRadio -> {
                viewModelScope.launch {
                    val artistTracks = repository.search(event.artist.name)
                    if (artistTracks.isNotEmpty()) {
                        val trackToPlay = artistTracks.first()
                        recommender.recordTrackPlay(trackToPlay)
                        musicController.playTrack(trackToPlay, artistTracks)
                    }
                }
            }

            is HomeEvent.PlayPlaylist -> {
                if (event.playlist.tracks.isNotEmpty()) {
                    val trackToPlay = event.playlist.tracks.first()
                    recommender.recordTrackPlay(trackToPlay)
                    musicController.playTrack(trackToPlay, event.playlist.tracks)
                } else {
                    viewModelScope.launch {
                        val tracks = repository.search(event.playlist.name)
                        if (tracks.isNotEmpty()) {
                            val trackToPlay = tracks.first()
                            recommender.recordTrackPlay(trackToPlay)
                            musicController.playTrack(trackToPlay, tracks)
                        } else if (allTracksCache.isNotEmpty()) {
                            val trackToPlay = allTracksCache.first()
                            recommender.recordTrackPlay(trackToPlay)
                            musicController.playTrack(trackToPlay, allTracksCache)
                        }
                    }
                }
            }

            is HomeEvent.PlayHero -> {
                viewModelScope.launch {
                    val targetId = event.banner.targetPlaylistId
                    val matchedPlaylist = allPlaylistsCache.find { it.id == targetId }
                        ?: SampleCatalog.samplePlaylists.find { it.id == targetId }

                    if (matchedPlaylist != null && matchedPlaylist.tracks.isNotEmpty()) {
                        val trackToPlay = matchedPlaylist.tracks.first()
                        recommender.recordTrackPlay(trackToPlay)
                        musicController.playTrack(trackToPlay, matchedPlaylist.tracks)
                    } else {
                        val tracks = repository.search(event.banner.targetQuery ?: event.banner.title)
                        val tracksToUse = if (tracks.isNotEmpty()) tracks else if (allTracksCache.isNotEmpty()) allTracksCache else SampleCatalog.sampleTracks
                        val trackToPlay = tracksToUse.first()
                        recommender.recordTrackPlay(trackToPlay)
                        musicController.playTrack(trackToPlay, tracksToUse)
                    }
                }
            }

            is HomeEvent.PlayTrack -> {
                recommender.recordTrackPlay(event.track)
                musicController.playTrack(event.track, event.queue)
            }
        }
    }
}
