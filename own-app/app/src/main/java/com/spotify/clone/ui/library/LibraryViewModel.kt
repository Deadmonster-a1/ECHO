package com.spotify.clone.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.mvi.UiEvent
import com.spotify.clone.core.mvi.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryState(
    val selectedFilter: String = "All",
    val isGridView: Boolean = false,
    val likedTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList()
) : UiState

sealed interface LibraryEvent : UiEvent {
    data class SelectFilter(val filter: String) : LibraryEvent
    data class CreatePlaylist(val name: String) : LibraryEvent
    object ToggleViewMode : LibraryEvent
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)

    private val _uiState = MutableStateFlow(LibraryState())
    val uiState: StateFlow<LibraryState> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            repository.getUserPlaylists().collect { userPlaylists ->
                _uiState.update { it.copy(playlists = userPlaylists) }
            }
        }
        viewModelScope.launch {
            repository.getFavoriteTracks().collect { favorites ->
                _uiState.update { it.copy(likedTracks = favorites) }
            }
        }
    }

    fun dispatch(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
            is LibraryEvent.ToggleViewMode -> {
                _uiState.update { it.copy(isGridView = !it.isGridView) }
            }
            is LibraryEvent.CreatePlaylist -> {
                viewModelScope.launch {
                    val newPlaylist = repository.createPlaylist(event.name)
                    _uiState.update { current ->
                        val updated = (listOf(newPlaylist) + current.playlists).distinctBy { it.id }
                        current.copy(playlists = updated)
                    }
                }
            }
        }
    }
}
