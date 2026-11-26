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
    val searchQuery: String = "",
    val isGridView: Boolean = false,
    val likedTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val localTracks: List<Track> = emptyList(),
    val isScanningLocal: Boolean = false
) : UiState

sealed interface LibraryEvent : UiEvent {
    data class SelectFilter(val filter: String) : LibraryEvent
    data class SearchQueryChanged(val query: String) : LibraryEvent
    data class CreatePlaylist(val name: String) : LibraryEvent
    data class RenamePlaylist(val playlistId: String, val name: String, val description: String = "") : LibraryEvent
    data class DeletePlaylist(val playlistId: String) : LibraryEvent
    data object ToggleViewMode : LibraryEvent
    data object ScanLocalAudio : LibraryEvent
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)

    private val _uiState = MutableStateFlow(LibraryState())
    val uiState: StateFlow<LibraryState> = _uiState.asStateFlow()

    init {
        loadLibrary()
        scanLocalFiles()
        viewModelScope.launch {
            AudioRepository.localScanRevision.collect { rev ->
                if (rev > 0L) {
                    scanLocalFiles(forceRefresh = true)
                }
            }
        }
    }

    fun scanLocalFiles(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLocal = true) }
            val locals = runCatching { repository.scanLocalAudioFiles(forceRefresh) }.getOrDefault(emptyList())
            _uiState.update { it.copy(localTracks = locals, isScanningLocal = false) }
        }
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
            is LibraryEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
            is LibraryEvent.SearchQueryChanged -> _uiState.update { it.copy(searchQuery = event.query) }
            is LibraryEvent.ToggleViewMode -> _uiState.update { it.copy(isGridView = !it.isGridView) }
            is LibraryEvent.ScanLocalAudio -> scanLocalFiles(forceRefresh = true)
            is LibraryEvent.CreatePlaylist -> {
                val name = event.name.trim()
                if (name.isBlank()) return
                viewModelScope.launch {
                    val created = repository.createPlaylist(name)
                    _uiState.update { current ->
                        current.copy(playlists = (listOf(created) + current.playlists).distinctBy { it.id })
                    }
                }
            }
            is LibraryEvent.RenamePlaylist -> {
                val cleanName = event.name.trim()
                if (cleanName.isBlank()) return
                viewModelScope.launch {
                    repository.updatePlaylistDetails(event.playlistId, cleanName, event.description)
                }
            }
            is LibraryEvent.DeletePlaylist -> {
                viewModelScope.launch {
                    repository.deletePlaylist(event.playlistId)
                }
            }
        }
    }
}
