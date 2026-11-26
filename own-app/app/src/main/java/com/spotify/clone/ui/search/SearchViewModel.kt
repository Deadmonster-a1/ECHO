package com.spotify.clone.ui.search

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.data.SearchHistoryRepository
import com.spotify.clone.core.data.SearchRankingEngine
import com.spotify.clone.core.data.SearchSuggestionService
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.designsystem.CategoryDance
import com.spotify.clone.core.designsystem.CategoryHipHop
import com.spotify.clone.core.designsystem.CategoryIndie
import com.spotify.clone.core.designsystem.CategoryMood
import com.spotify.clone.core.designsystem.CategoryPop
import com.spotify.clone.core.designsystem.CategoryRock
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.mvi.UiEvent
import com.spotify.clone.core.mvi.UiState
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.CategoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SearchTab(val title: String) {
    ALL("All"),
    TRACKS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    LOCAL("Local")
}

data class SearchResultsBundle(
    val tracks: List<Track> = emptyList(),
    val albums: List<Playlist> = emptyList(),
    val artists: List<String> = emptyList(),
    val localTracks: List<Track> = emptyList()
)

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val selectedTab: SearchTab = SearchTab.ALL,
    val searchResults: List<Track> = emptyList(),
    val searchAlbums: List<Playlist> = emptyList(),
    val searchArtists: List<String> = emptyList(),
    val localAudioTracks: List<Track> = emptyList(),
    val liveSuggestions: List<String> = emptyList(),
    val songRecommendations: List<Track> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isShowingSuggestions: Boolean = false,
    val categories: List<CategoryItem> = listOf(
        CategoryItem("pop", "Global Pop", CategoryPop, "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80", "Pop Hits"),
        CategoryItem("hiphop", "Hip-Hop", CategoryHipHop, "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80", "Hip-Hop"),
        CategoryItem("bollywood", "Bollywood", Color(0xFFE55B3C), "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", "Hindi Top Hits"),
        CategoryItem("telugu", "Telugu", Color(0xFFD64040), "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg", "Telugu Hits"),
        CategoryItem("tamil", "Tamil", Color(0xFF8E44AD), "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg", "Tamil Top Hits"),
        CategoryItem("punjabi", "Punjabi", Color(0xFFF97316), "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg", "Punjabi Hits"),
        CategoryItem("kpop", "K-Pop", Color(0xFFEC4899), "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80", "K-Pop Hits"),
        CategoryItem("latin", "Latin", Color(0xFFE11D48), "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80", "Latin Top Hits"),
        CategoryItem("rock", "Rock", CategoryRock, "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80", "Rock"),
        CategoryItem("indie", "Indie", CategoryIndie, "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80", "Indie"),
        CategoryItem("dance", "Dance / EDM", CategoryDance, "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80", "Dance"),
        CategoryItem("chill", "Chill & Lo-Fi", CategoryMood, "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg", "Chill Lo-Fi"),
        CategoryItem("romance", "Romance", Color(0xFFE84393), "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg", "Romance"),
        CategoryItem("classical", "Classical", Color(0xFF16A085), "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80", "Classical")
    )
) : UiState

sealed interface SearchEvent : UiEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class ExecuteSearch(val query: String) : SearchEvent
    data class SelectSuggestion(val suggestion: String) : SearchEvent
    data class TabSelected(val tab: SearchTab) : SearchEvent
    data class PlayTrack(val track: Track, val queue: List<Track>) : SearchEvent
    data class RemoveRecentSearch(val query: String) : SearchEvent
    data object ClearRecentSearches : SearchEvent
    data object ClearQuery : SearchEvent
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private val historyRepository = SearchHistoryRepository.getInstance(application)
    private val musicController = MusicController.getInstance(application)

    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null
    @Volatile private var favoriteTrackIds: Set<String> = emptySet()

    private val searchCacheLock = Any()
    private val searchCache = object : LinkedHashMap<String, SearchResultsBundle>(40, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchResultsBundle>?): Boolean = size > 40
    }

    init {
        viewModelScope.launch {
            historyRepository.history.collect { history ->
                _uiState.update { it.copy(recentSearches = history) }
            }
        }
        viewModelScope.launch {
            repository.getFavoriteTracks().collect { favorites ->
                val favIds = favorites.mapTo(HashSet()) { it.id }
                favoriteTrackIds = favIds
                val syncFav: (Track) -> Track = { t ->
                    val fav = t.id in favIds
                    if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
                }
                _uiState.update { current ->
                    current.copy(
                        searchResults = current.searchResults.map(syncFav),
                        songRecommendations = current.songRecommendations.map(syncFav),
                        localAudioTracks = current.localAudioTracks.map(syncFav)
                    )
                }
            }
        }
    }

    fun dispatch(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> handleQueryChanged(event.query)
            is SearchEvent.ExecuteSearch -> executeDirectSearch(event.query)
            is SearchEvent.SelectSuggestion -> executeDirectSearch(event.suggestion)
            is SearchEvent.TabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }
            is SearchEvent.PlayTrack -> musicController.playTrack(event.track, event.queue)
            is SearchEvent.RemoveRecentSearch -> historyRepository.remove(event.query)
            is SearchEvent.ClearRecentSearches -> historyRepository.clear()
            is SearchEvent.ClearQuery -> {
                suggestionsJob?.cancel()
                searchJob?.cancel()
                _uiState.update {
                    it.copy(
                        query = "",
                        isSearching = false,
                        isShowingSuggestions = false,
                        liveSuggestions = emptyList(),
                        songRecommendations = emptyList(),
                        searchResults = emptyList(),
                        searchAlbums = emptyList(),
                        searchArtists = emptyList(),
                        localAudioTracks = emptyList()
                    )
                }
            }
        }
    }

    private fun handleQueryChanged(newQuery: String) {
        _uiState.update {
            it.copy(query = newQuery, isShowingSuggestions = newQuery.isNotBlank())
        }
        suggestionsJob?.cancel()
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            dispatch(SearchEvent.ClearQuery)
            return
        }

        suggestionsJob = viewModelScope.launch {
            delay(120)
            val (suggestions, instantSongs) = withContext(Dispatchers.IO) {
                val suggs = SearchSuggestionService.getSuggestions(newQuery)
                val q = newQuery.trim().lowercase()
                val favIds = favoriteTrackIds
                val songs = SearchRankingEngine.rankTracks(
                    newQuery,
                    (WorldOnboardingCatalog.allOnboardingTracks() + SampleCatalog.sampleTracks + repository.scanLocalAudioFiles())
                        .filter {
                            it.title.lowercase().contains(q) ||
                                it.artistName.lowercase().contains(q) ||
                                SearchRankingEngine.normalizedSimilarity(q, it.title) >= 0.45
                        }
                        .distinctBy { "${it.title.lowercase()}_${it.artistName.lowercase()}" }
                ).take(5).map { t ->
                    val fav = t.id in favIds
                    if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
                }
                suggs to songs
            }

            if (_uiState.value.query == newQuery) {
                _uiState.update {
                    it.copy(
                        liveSuggestions = suggestions,
                        songRecommendations = instantSongs,
                        isShowingSuggestions = true
                    )
                }
            }
        }

        searchJob = viewModelScope.launch {
            delay(350)
            performSearch(newQuery, saveToHistory = false)
        }
    }

    private fun executeDirectSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        suggestionsJob?.cancel()
        searchJob?.cancel()
        historyRepository.add(trimmed)
        _uiState.update { it.copy(query = trimmed, isShowingSuggestions = false) }
        searchJob = viewModelScope.launch {
            performSearch(trimmed, saveToHistory = true)
        }
    }

    private suspend fun performSearch(query: String, saveToHistory: Boolean) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        if (saveToHistory) historyRepository.add(trimmed)

        val cacheKey = trimmed.lowercase()
        synchronized(searchCacheLock) { searchCache[cacheKey] }?.let { cached ->
            val favIds = favoriteTrackIds
            val syncFav: (Track) -> Track = { t ->
                val fav = t.id in favIds
                if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
            }
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = cached.tracks.map(syncFav),
                    searchAlbums = cached.albums,
                    searchArtists = cached.artists,
                    localAudioTracks = cached.localTracks.map(syncFav)
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true) }

        val (tracks, albums, artists, local) = coroutineScope {
            val tracksDeferred = async(Dispatchers.IO) {
                runCatching { repository.search(trimmed) }.getOrDefault(emptyList())
            }
            val albumsDeferred = async(Dispatchers.IO) {
                runCatching { repository.searchAlbums(trimmed) }.getOrDefault(emptyList())
            }
            val localDeferred = async(Dispatchers.IO) {
                runCatching {
                    val q = trimmed.lowercase()
                    repository.scanLocalAudioFiles().filter {
                        it.title.lowercase().contains(q) ||
                            it.artistName.lowercase().contains(q) ||
                            it.albumName.lowercase().contains(q)
                    }
                }.getOrDefault(emptyList())
            }

            val rankedTracks = SearchRankingEngine.rankTracks(trimmed, tracksDeferred.await())
            val fetchedAlbums = albumsDeferred.await()
            val rankedLocal = SearchRankingEngine.rankTracks(trimmed, localDeferred.await())
            val fetchedArtists = rankedTracks
                .flatMap { WorldOnboardingCatalog.extractArtistTokens(it.artistName) }
                .distinctBy { it.lowercase() }
            SearchResultsBundle(rankedTracks, fetchedAlbums, fetchedArtists, rankedLocal)
        }

        if (_uiState.value.query.trim() != trimmed) return

        val favIds = favoriteTrackIds
        val syncFav: (Track) -> Track = { t ->
            val fav = t.id in favIds
            if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
        }
        val syncedTracks = tracks.map(syncFav)
        val syncedLocal = local.map(syncFav)

        synchronized(searchCacheLock) {
            searchCache[cacheKey] = SearchResultsBundle(syncedTracks, albums, artists, syncedLocal)
        }

        _uiState.update {
            it.copy(
                isSearching = false,
                searchResults = syncedTracks,
                searchAlbums = albums,
                searchArtists = artists,
                localAudioTracks = syncedLocal
            )
        }
    }
}
