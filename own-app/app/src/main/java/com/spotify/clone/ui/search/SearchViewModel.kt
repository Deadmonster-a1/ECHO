package com.spotify.clone.ui.search

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.CategoryDance
import com.spotify.clone.core.designsystem.CategoryDiscover
import com.spotify.clone.core.designsystem.CategoryHipHop
import com.spotify.clone.core.designsystem.CategoryIndie
import com.spotify.clone.core.designsystem.CategoryMood
import com.spotify.clone.core.designsystem.CategoryPodcasts
import com.spotify.clone.core.designsystem.CategoryPop
import com.spotify.clone.core.designsystem.CategoryRock
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.mvi.UiEvent
import com.spotify.clone.core.mvi.UiState
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.CategoryItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val selectedFilter: String = "All",
    val searchResults: List<Track> = emptyList(),
    val searchAlbums: List<Playlist> = emptyList(),
    val trendingSuggestions: List<String> = listOf(
        "Chuttamalle",
        "Sid Sriram",
        "Pushpa 2",
        "Devara",
        "Anirudh",
        "Arijit Singh",
        "Coldplay",
        "Taylor Swift"
    ),
    val categories: List<CategoryItem> = listOf(
        CategoryItem(
            id = "telugu",
            title = "Telugu Hits",
            backgroundColor = Color(0xFFD64040),
            imageUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            searchQuery = "Telugu"
        ),
        CategoryItem(
            id = "pop",
            title = "Pop",
            backgroundColor = CategoryPop,
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/49/3d/ab/493dab54-f920-9043-6181-80993b8116c9/19UMGIM53909.rgb.jpg/600x600bb.jpg",
            searchQuery = "Pop"
        ),
        CategoryItem(
            id = "hiphop",
            title = "Hip-Hop",
            backgroundColor = CategoryHipHop,
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music118/v4/dd/5c/e6/dd5ce621-f7d2-f767-7a08-e7a7eaa7870b/00602537526994.rgb.jpg/600x600bb.jpg",
            searchQuery = "Hip-Hop"
        ),
        CategoryItem(
            id = "bollywood",
            title = "Bollywood",
            backgroundColor = Color(0xFFE55B3C),
            imageUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
            searchQuery = "Bollywood"
        ),
        CategoryItem(
            id = "punjabi",
            title = "Punjabi",
            backgroundColor = Color(0xFFE67E22),
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/79/d2/01/79d201d2-e54d-5604-81fb-313f30db7219/198588533581.jpg/600x600bb.jpg",
            searchQuery = "Punjabi"
        ),
        CategoryItem(
            id = "rock",
            title = "Rock",
            backgroundColor = CategoryRock,
            imageUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
            searchQuery = "Rock"
        ),
        CategoryItem(
            id = "indie",
            title = "Indie",
            backgroundColor = CategoryIndie,
            imageUrl = "https://c.saavncdn.com/000/Balagam-Telugu-2023-20230304184334-500x500.jpg",
            searchQuery = "Indie"
        ),
        CategoryItem(
            id = "dance",
            title = "Dance / EDM",
            backgroundColor = CategoryDance,
            imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            searchQuery = "Dance"
        ),
        CategoryItem(
            id = "romance",
            title = "Romance",
            backgroundColor = Color(0xFFE84393),
            imageUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            searchQuery = "Romance"
        ),
        CategoryItem(
            id = "mood",
            title = "Mood & Chill",
            backgroundColor = CategoryMood,
            imageUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
            searchQuery = "Mood"
        ),
        CategoryItem(
            id = "podcasts",
            title = "Podcasts",
            backgroundColor = CategoryPodcasts,
            imageUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&q=80",
            searchQuery = "Podcast"
        ),
        CategoryItem(
            id = "charts",
            title = "Charts",
            backgroundColor = Color(0xFF5B69A8),
            imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80",
            searchQuery = "Top Hits"
        ),
        CategoryItem(
            id = "discover",
            title = "Discover",
            backgroundColor = CategoryDiscover,
            imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            searchQuery = "Discover"
        ),
        CategoryItem(
            id = "party",
            title = "Party & Mass",
            backgroundColor = Color(0xFFE17055),
            imageUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
            searchQuery = "Mass"
        ),
        CategoryItem(
            id = "devotional",
            title = "Devotional",
            backgroundColor = Color(0xFFF39C12),
            imageUrl = "https://images.unsplash.com/photo-1606293926075-69a00dbfde81?w=500&q=80",
            searchQuery = "Devotional"
        ),
        CategoryItem(
            id = "lofi",
            title = "Late Night Lo-Fi",
            backgroundColor = Color(0xFF6C5CE7),
            imageUrl = "https://c.saavncdn.com/237/Geetha-Govindam-Telugu-2018-20180921-500x500.jpg",
            searchQuery = "Lo-Fi"
        )
    )
) : UiState

sealed interface SearchEvent : UiEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class FilterSelected(val filter: String) : SearchEvent
    data class PlayTrack(val track: Track, val queue: List<Track>) : SearchEvent
    object ClearQuery : SearchEvent
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private val musicController = MusicController.getInstance(application)

    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun dispatch(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                _uiState.update { it.copy(query = event.query) }
                searchJob?.cancel()
                if (event.query.isNotBlank()) {
                    searchJob = viewModelScope.launch {
                        delay(250) // debounce typing
                        _uiState.update { it.copy(isSearching = true) }
                        val tracksDeferred = async { try { repository.search(event.query) } catch (e: Exception) { emptyList() } }
                        val albumsDeferred = async { try { repository.searchAlbums(event.query) } catch (e: Exception) { emptyList() } }
                        val tracks = tracksDeferred.await()
                        val albums = albumsDeferred.await()
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                searchResults = tracks,
                                searchAlbums = albums
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), searchAlbums = emptyList()) }
                }
            }
            is SearchEvent.FilterSelected -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
            is SearchEvent.ClearQuery -> {
                searchJob?.cancel()
                _uiState.update { it.copy(query = "", isSearching = false, searchResults = emptyList(), searchAlbums = emptyList()) }
            }
            is SearchEvent.PlayTrack -> {
                musicController.playTrack(event.track, event.queue)
            }
        }
    }
}
