package com.spotify.clone.core.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.FullTrackStreamResolver
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.model.Track
import com.spotify.clone.widget.EchoWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Authoritative Music Controller for ECHO
 * Connects to PlaybackService MediaSession, coordinates playback state across the app,
 * handles track pre-resolution, queue modifications, and pushes updates to home-screen widgets.
 */
class MusicController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var pendingAction: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val repository = AudioRepository(context.applicationContext)
    private var currentQueue: List<Track> = emptyList()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                Log.d(TAG, "Connected to PlaybackService MediaController: $controller")
                controller?.let { player ->
                    restoreQueueFromPlayer(player)
                    setupPlayerListener(player)
                    updateState()
                }
                pendingAction?.invoke()
                pendingAction = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun restoreQueueFromPlayer(player: MediaController) {
        if (player.mediaItemCount > 0 && currentQueue.isEmpty()) {
            val restored = mutableListOf<Track>()
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i)
                val metadata = item.mediaMetadata
                restored.add(
                    Track(
                        id = item.mediaId.ifBlank { "media_$i" },
                        title = metadata.title?.toString() ?: "Track $i",
                        artistName = metadata.artist?.toString() ?: "Unknown Artist",
                        albumName = metadata.albumTitle?.toString() ?: "",
                        coverArtUrl = metadata.artworkUri?.toString() ?: "",
                        durationMs = if (i == player.currentMediaItemIndex) player.duration.coerceAtLeast(0L) else 0L,
                        mediaUri = item.requestMetadata.mediaUri?.toString() ?: ""
                    )
                )
            }
            currentQueue = restored
            Log.d(TAG, "Restored active queue of ${restored.size} tracks from running player")
        }
    }

    private fun setupPlayerListener(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                if (isPlaying) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isLoading = playbackState == Player.STATE_BUFFERING
                _playbackState.update { it.copy(isLoading = isLoading) }
                updateState()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error in MusicController: ${error.errorCodeName} - ${error.message}", error)
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = "Playback issue: ${error.errorCodeName}"
                    )
                }
                val p = controller ?: return
                if (p.hasNextMediaItem()) {
                    Log.d(TAG, "Recovering from playback error by advancing to next track")
                    p.seekToNextMediaItem()
                    p.prepare()
                    p.play()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val track = currentQueue.find { it.id == mediaId }
                _playbackState.update { it.copy(currentTrack = track, errorMessage = null) }
                updateState()

                if (track != null && !TrackDownloadManager.getInstance(context).isTrackDownloaded(track.id) && FullTrackStreamResolver.needsResolution(track)) {
                    scope.launch(Dispatchers.IO) {
                        val resolvedUrl = if (track.mediaUri.startsWith("saavn_enc:")) {
                            val encUrl = track.mediaUri.removePrefix("saavn_enc:")
                            FullTrackStreamResolver.resolveAuthToken(encUrl)
                                ?: FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
                        } else {
                            FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
                        }
                        if (!resolvedUrl.isNullOrBlank() && resolvedUrl != track.mediaUri) {
                            val resolvedTrack = track.copy(mediaUri = resolvedUrl)
                            val idx = currentQueue.indexOfFirst { it.id == track.id }
                            if (idx != -1) {
                                val updated = currentQueue.toMutableList()
                                updated[idx] = resolvedTrack
                                currentQueue = updated
                                withContext(Dispatchers.Main) {
                                    val currentPos = controller?.currentPosition ?: 0L
                                    controller?.replaceMediaItem(idx, resolvedTrack.toMediaItem())
                                    if (controller?.currentMediaItemIndex == idx) {
                                        controller?.seekTo(idx, currentPos)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playbackState.update { it.copy(repeatMode = mode) }
            }
        })
    }

    private fun updateState() {
        val player = controller ?: return
        val currentTrack = currentQueue.getOrNull(player.currentMediaItemIndex)
            ?: if (player.currentMediaItem != null) {
                val meta = player.currentMediaItem!!.mediaMetadata
                Track(
                    id = player.currentMediaItem!!.mediaId,
                    title = meta.title?.toString() ?: "Track",
                    artistName = meta.artist?.toString() ?: "Unknown",
                    albumName = meta.albumTitle?.toString() ?: "",
                    coverArtUrl = meta.artworkUri?.toString() ?: "",
                    durationMs = player.duration.coerceAtLeast(0L),
                    mediaUri = ""
                )
            } else null

        _playbackState.update {
            it.copy(
                currentTrack = currentTrack,
                isPlaying = player.isPlaying,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                queue = currentQueue,
                queueIndex = player.currentMediaItemIndex
            )
        }

        // Push synchronization to home screen widgets
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration.coerceAtLeast(0L)
                        val buf = player.bufferedPosition.coerceAtLeast(0L)
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = pos,
                                bufferedPositionMs = buf,
                                durationMs = dur
                            )
                        }
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        Log.d(TAG, "playTrack requested: ${track.title} (${track.mediaUri})")
        if (track.mediaUri.startsWith("spotify:")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(track.mediaUri)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        // Optimistically update playback state immediately
        _playbackState.update {
            it.copy(
                currentTrack = track,
                isPlaying = true,
                isLoading = true,
                queue = queue,
                queueIndex = startIndex,
                errorMessage = null
            )
        }

        scope.launch(Dispatchers.IO) {
            val downloadMgr = TrackDownloadManager.getInstance(context)
            val updatedQueue = queue.map { t ->
                if (downloadMgr.isTrackDownloaded(t.id)) {
                    downloadMgr.resolveTrackForPlayback(t)
                } else {
                    t
                }
            }.toMutableList()

            val targetTrack = updatedQueue.getOrNull(startIndex) ?: track
            val resolvedTrack = if (downloadMgr.isTrackDownloaded(targetTrack.id)) {
                Log.d(TAG, "Playing downloaded offline file for '${targetTrack.title}'")
                downloadMgr.resolveTrackForPlayback(targetTrack)
            } else if (FullTrackStreamResolver.needsResolution(targetTrack)) {
                Log.d(TAG, "Track '${targetTrack.title}' needs stream resolution... uri: ${targetTrack.mediaUri}")
                val resolvedUrl = if (targetTrack.mediaUri.startsWith("saavn_enc:")) {
                    val encUrl = targetTrack.mediaUri.removePrefix("saavn_enc:")
                    FullTrackStreamResolver.resolveAuthToken(encUrl)
                        ?: FullTrackStreamResolver.resolveStreamUrl(targetTrack.title, targetTrack.artistName, targetTrack.albumName)
                } else {
                    FullTrackStreamResolver.resolveStreamUrl(targetTrack.title, targetTrack.artistName, targetTrack.albumName)
                }
                if (!resolvedUrl.isNullOrBlank()) {
                    Log.d(TAG, "Resolved full studio stream for '${targetTrack.title}': $resolvedUrl")
                    targetTrack.copy(mediaUri = resolvedUrl)
                } else {
                    targetTrack
                }
            } else {
                targetTrack
            }

            if (startIndex in updatedQueue.indices) {
                updatedQueue[startIndex] = resolvedTrack
            }
            currentQueue = updatedQueue

            val mediaItems = updatedQueue.map { it.toMediaItem() }

            withContext(Dispatchers.Main) {
                val player = controller
                if (player != null) {
                    player.setMediaItems(mediaItems, startIndex, 0L)
                    player.prepare()
                    player.play()
                    Log.d(TAG, "Sent setMediaItems, prepare, play to MediaController")
                } else {
                    Log.d(TAG, "Controller not ready yet, queuing pending play action")
                    pendingAction = {
                        controller?.let {
                            it.setMediaItems(mediaItems, startIndex, 0L)
                            it.prepare()
                            it.play()
                        }
                    }
                }

                _playbackState.update {
                    it.copy(
                        currentTrack = resolvedTrack,
                        isPlaying = true,
                        isLoading = false,
                        queue = updatedQueue,
                        queueIndex = startIndex,
                        errorMessage = null
                    )
                }
                EchoWidgetManager.updateAll(context, _playbackState.value)
                startProgressPolling()
            }

            // In background, pre-resolve next tracks in queue so skipNext is instantaneous
            val nextIndex = startIndex + 1
            if (nextIndex < updatedQueue.size) {
                val nextTrack = updatedQueue[nextIndex]
                if (!downloadMgr.isTrackDownloaded(nextTrack.id) && FullTrackStreamResolver.needsResolution(nextTrack)) {
                    val nextUrl = if (nextTrack.mediaUri.startsWith("saavn_enc:")) {
                        val enc = nextTrack.mediaUri.removePrefix("saavn_enc:")
                        FullTrackStreamResolver.resolveAuthToken(enc)
                            ?: FullTrackStreamResolver.resolveStreamUrl(nextTrack.title, nextTrack.artistName, nextTrack.albumName)
                    } else {
                        FullTrackStreamResolver.resolveStreamUrl(nextTrack.title, nextTrack.artistName, nextTrack.albumName)
                    }
                    if (!nextUrl.isNullOrBlank()) {
                        val nextResolved = nextTrack.copy(mediaUri = nextUrl)
                        updatedQueue[nextIndex] = nextResolved
                        currentQueue = updatedQueue
                        withContext(Dispatchers.Main) {
                            controller?.replaceMediaItem(nextIndex, nextResolved.toMediaItem())
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.let {
            if (it.currentPosition > 3000L) {
                it.seekTo(0L)
            } else {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    fun toggleShuffle() {
        controller?.let {
            val nextShuffle = !it.shuffleModeEnabled
            it.shuffleModeEnabled = nextShuffle
        }
    }

    fun toggleRepeat() {
        controller?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }

    fun toggleFavorite(track: Track) {
        scope.launch(Dispatchers.IO) {
            val newStatus = repository.toggleFavorite(track)
            withContext(Dispatchers.Main) {
                _playbackState.update { state ->
                    val updatedCurrent = if (state.currentTrack?.id == track.id) {
                        state.currentTrack.copy(isFavorite = newStatus)
                    } else state.currentTrack

                    val updatedQueue = state.queue.map {
                        if (it.id == track.id) it.copy(isFavorite = newStatus) else it
                    }

                    state.copy(currentTrack = updatedCurrent, queue = updatedQueue)
                }
            }
        }
    }

    fun addToQueue(track: Track) {
        currentQueue = currentQueue + track
        controller?.addMediaItem(track.toMediaItem())
        _playbackState.update { it.copy(queue = currentQueue) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val updated = currentQueue.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        currentQueue = updated
        controller?.moveMediaItem(fromIndex, toIndex)
        _playbackState.update { it.copy(queue = currentQueue, queueIndex = controller?.currentMediaItemIndex ?: it.queueIndex) }
    }

    fun removeFromQueue(index: Int) {
        if (index !in currentQueue.indices) return
        val updated = currentQueue.toMutableList()
        updated.removeAt(index)
        currentQueue = updated
        controller?.removeMediaItem(index)
        _playbackState.update { it.copy(queue = currentQueue, queueIndex = controller?.currentMediaItemIndex ?: it.queueIndex) }
    }

    fun playQueueIndex(index: Int) {
        if (index !in currentQueue.indices) return
        controller?.seekTo(index, 0L)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var current = totalSeconds
            while (current > 0 && isActive) {
                delay(1000)
                current--
                _sleepTimerRemainingSeconds.value = current
            }
            if (current <= 0) {
                Log.d(TAG, "Sleep timer finished, pausing playback")
                controller?.pause()
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        controller?.setPlaybackSpeed(speed)
    }

    private fun Track.toMediaItem(): MediaItem {
        val artUri = if (coverArtUrl.isNotBlank()) Uri.parse(coverArtUrl) else null
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artistName)
            .setAlbumTitle(albumName)
            .setArtworkUri(artUri)
            .build()

        val safeUri = if (mediaUri.isNotBlank() && 
            !mediaUri.startsWith("saavn_enc:") && 
            !mediaUri.contains("codeskulptor") && 
            !mediaUri.contains("googleapis.com")) {
            mediaUri
        } else {
            ""
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(if (safeUri.isNotBlank()) safeUri else "about:blank")
            .setMediaMetadata(metadata)
            .build()
    }

    companion object {
        private const val TAG = "EchoMusicController"

        @Volatile
        private var INSTANCE: MusicController? = null

        fun getInstance(context: Context): MusicController {
            return INSTANCE ?: synchronized(this) {
                val instance = MusicController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
