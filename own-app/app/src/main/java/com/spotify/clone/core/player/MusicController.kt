package com.spotify.clone.core.player

import android.content.ComponentName
import android.content.Context
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
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.recommendation.EchoTasteMlModel
import com.spotify.clone.core.recommendation.TasteSignalType
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.widget.EchoWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.abs

/**
 * Clean, authoritative playback controller for ECHO.
 * Connects to PlaybackService MediaSession, resolves offline/online streams,
 * enforces Spotify-style queue ordering (artist-balanced shuffle, unplayed-first,
 * Play Next splice) and Same-Genre Smart Radio auto-continuation.
 */
class MusicController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var continuationJob: Job? = null
    private val resolvingTrackIds: MutableSet<String> = Collections.synchronizedSet(HashSet())
    @Volatile private var queueGenerationId: Long = 0L
    @Volatile private var lastCompletedTrackId: String? = null
    private var pendingAction: (() -> Unit)? = null

    private val repository = AudioRepository(context.applicationContext)
    private var currentQueue: List<Track> = emptyList()
    private var originalQueueOrder: List<Track> = emptyList()
    private var isSpotifyShuffleEnabled: Boolean = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var sleepTimerEndOfTrack: Boolean = false
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isSmartRadioEnabled = MutableStateFlow(true)
    val isSmartRadioEnabled: StateFlow<Boolean> = _isSmartRadioEnabled.asStateFlow()

    private val _activeGenreLock = MutableStateFlow(MusicGenre.ALL)
    val activeGenreLock: StateFlow<MusicGenre> = _activeGenreLock.asStateFlow()

    private val _autoMatchGenre = MutableStateFlow(true)
    val autoMatchGenre: StateFlow<Boolean> = _autoMatchGenre.asStateFlow()

    init {
        SettingsManager.init(context)
        EchoRecommendationEngine.init(context)
        _playbackSpeed.value = SettingsManager.playbackSpeed.value
        _isSmartRadioEnabled.value = SettingsManager.smartRadioEnabled.value
        _activeGenreLock.value = SettingsManager.selectedGenreLock.value
        _autoMatchGenre.value = SettingsManager.autoMatchGenre.value
        initializeController()
        observeSettings()
    }

    private fun observeSettings() {
        scope.launch {
            SettingsManager.playbackSpeed.collectLatest { speed ->
                if (abs(_playbackSpeed.value - speed) > 0.01f) {
                    _playbackSpeed.value = speed
                    controller?.setPlaybackSpeed(speed)
                }
            }
        }
        scope.launch {
            SettingsManager.smartRadioEnabled.collectLatest { enabled ->
                _isSmartRadioEnabled.value = enabled
            }
        }
        scope.launch {
            SettingsManager.selectedGenreLock.collectLatest { genre ->
                _activeGenreLock.value = genre
            }
        }
        scope.launch {
            SettingsManager.autoMatchGenre.collectLatest { auto ->
                _autoMatchGenre.value = auto
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val player = controllerFuture?.get() ?: return@addListener
                controller = player
                // Keep ExoPlayer linear so MusicController's Spotify-ordered permutation stays authoritative
                player.shuffleModeEnabled = false
                restoreQueueFromPlayer(player)
                setupPlayerListener(player)
                if (abs(_playbackSpeed.value - 1.0f) > 0.01f) {
                    player.setPlaybackSpeed(_playbackSpeed.value)
                }
                updateState()
                pendingAction?.invoke()
                pendingAction = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun restoreQueueFromPlayer(player: MediaController) {
        if (player.mediaItemCount <= 0 || currentQueue.isNotEmpty()) return
        currentQueue = (0 until player.mediaItemCount).map { i ->
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata
            Track(
                id = item.mediaId.ifBlank { "media_$i" },
                title = meta.title?.toString() ?: "Track $i",
                artistName = meta.artist?.toString() ?: "Unknown Artist",
                albumName = meta.albumTitle?.toString() ?: "",
                coverArtUrl = meta.artworkUri?.toString() ?: "",
                durationMs = if (i == player.currentMediaItemIndex) player.duration.coerceAtLeast(0L) else 0L,
                mediaUri = item.requestMetadata.mediaUri?.toString() ?: ""
            )
        }
        originalQueueOrder = currentQueue
    }

    private fun setupPlayerListener(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                if (isPlaying) startProgressPolling() else stopProgressPolling()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
                updateState()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.errorCodeName}", error)
                val current = _playbackState.value.currentTrack
                if (current != null && (current.mediaUri.isBlank() || FullTrackStreamResolver.needsResolution(current))) {
                    resolveTrackInBackgroundIfNeeded(current, queueGenerationId)
                    return
                }
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = "Playback failed (${error.errorCodeName})"
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (sleepTimerEndOfTrack) {
                    controller?.pause()
                    cancelSleepTimer()
                    return
                }

                val mediaId = mediaItem?.mediaId
                val track = currentQueue.find { it.id == mediaId }
                val genAtStart = queueGenerationId

                if (track != null) {
                    lastCompletedTrackId = null
                    _playbackState.update { it.copy(currentTrack = track, errorMessage = null) }
                    SmartRadioQueue.recordTrackPlayed(track)
                    EchoRecommendationEngine.recordTrackPlay(track)
                    applyAutoGenreEqIfEnabled(track)
                    scope.launch(Dispatchers.IO) {
                        try { repository.recordTrackPlayed(track) } catch (_: Exception) {}
                    }
                    maybeAppendSmartRadio(track, genAtStart)
                    resolveTrackInBackgroundIfNeeded(track, genAtStart)
                }
                updateState()
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

    fun getEffectiveGenre(track: Track? = _playbackState.value.currentTrack): MusicGenre {
        val lock = _activeGenreLock.value
        if (lock != MusicGenre.ALL) return lock
        if (_autoMatchGenre.value && track != null) return MusicGenre.inferGenre(track)
        return MusicGenre.ALL
    }

    private fun maybeAppendSmartRadio(currentTrack: Track, genAtStart: Long) {
        val currentIndex = controller?.currentMediaItemIndex ?: 0
        val remaining = currentQueue.size - 1 - currentIndex
        if (!_isSmartRadioEnabled.value || remaining > 2 || currentQueue.isEmpty() || continuationJob?.isActive == true) {
            return
        }
        val snapshot = currentQueue.toList()
        val locked = _activeGenreLock.value
        val autoMatch = _autoMatchGenre.value

        continuationJob = scope.launch(Dispatchers.IO) {
            val autoAdd = SmartRadioQueue.generateRadioContinuationAsync(
                seedTrack = currentTrack,
                currentQueue = snapshot,
                repository = repository,
                lockedGenre = locked,
                autoMatchGenre = autoMatch
            )
            if (autoAdd.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                if (genAtStart != queueGenerationId) return@withContext
                val existingIds = currentQueue.mapTo(HashSet()) { it.id }
                val unique = autoAdd.filter { existingIds.add(it.id) }
                if (unique.isNotEmpty()) {
                    currentQueue = currentQueue + unique
                    originalQueueOrder = originalQueueOrder + unique
                    unique.forEach { controller?.addMediaItem(it.toMediaItem()) }
                    updateState()
                }
            }
        }
    }

    private fun resolveTrackInBackgroundIfNeeded(track: Track, genAtStart: Long) {
        val downloadMgr = TrackDownloadManager.getInstance(context)
        if (downloadMgr.isTrackDownloaded(track.id) || !FullTrackStreamResolver.needsResolution(track)) return
        if (!resolvingTrackIds.add(track.id)) return

        scope.launch(Dispatchers.IO) {
            try {
                val resolved = resolvePlayableTrack(track, downloadMgr)
                withContext(Dispatchers.Main) {
                    if (genAtStart != queueGenerationId) return@withContext
                    val idx = currentQueue.indexOfFirst { it.id == track.id }
                    if (resolved.mediaUri.isNotBlank() && !resolved.mediaUri.startsWith("saavn_enc:") && resolved.mediaUri != track.mediaUri) {
                        if (idx != -1) {
                            val updated = currentQueue.toMutableList()
                            updated[idx] = resolved
                            currentQueue = updated
                            originalQueueOrder = originalQueueOrder.map { if (it.id == resolved.id) resolved else it }
                            val player = controller
                            val currentPos = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
                            val wasIdleOrErrored = player?.playbackState == Player.STATE_IDLE
                            player?.replaceMediaItem(idx, resolved.toMediaItem())
                            if (player?.currentMediaItemIndex == idx) {
                                player.seekTo(idx, currentPos)
                                if (wasIdleOrErrored) {
                                    player.prepare()
                                    player.play()
                                }
                            }
                            _playbackState.update { it.copy(currentTrack = resolved, errorMessage = null) }
                        }
                    } else if (controller?.currentMediaItemIndex == idx && controller?.playbackState == Player.STATE_IDLE) {
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                isLoading = false,
                                errorMessage = "Unable to stream this track. Check your connection."
                            )
                        }
                    }
                }
            } finally {
                resolvingTrackIds.remove(track.id)
            }
        }
    }

    private suspend fun resolvePlayableTrack(track: Track, downloadMgr: TrackDownloadManager): Track {
        if (downloadMgr.isTrackDownloaded(track.id)) {
            return downloadMgr.resolveTrackForPlayback(track)
        }
        if (!FullTrackStreamResolver.needsResolution(track)) {
            return track
        }
        return if (track.mediaUri.startsWith("saavn_enc:")) {
            val enc = track.mediaUri.removePrefix("saavn_enc:")
            val url = FullTrackStreamResolver.resolveAuthToken(enc, bitrate = "320")
                ?: FullTrackStreamResolver.resolveAuthToken(enc, bitrate = "160")
                ?: FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
            if (!url.isNullOrBlank()) track.copy(mediaUri = url) else track
        } else {
            val details = FullTrackStreamResolver.resolveTrackDetails(track.title, track.artistName, track.albumName)
            if (details != null && details.streamUrl.isNotBlank()) {
                track.copy(
                    mediaUri = details.streamUrl,
                    coverArtUrl = track.coverArtUrl.ifBlank { details.coverArtUrl ?: "" },
                    albumName = if (track.albumName.isBlank() || track.albumName.equals("Single", ignoreCase = true)) {
                        details.albumName ?: track.albumName
                    } else track.albumName,
                    durationMs = details.durationMs?.takeIf { it > 0L } ?: track.durationMs
                )
            } else {
                track
            }
        }
    }

    private fun updateState() {
        val player = controller ?: return
        val activeMediaId = player.currentMediaItem?.mediaId
        val currentTrack = currentQueue.find { it.id == activeMediaId }
            ?: currentQueue.getOrNull(player.currentMediaItemIndex)
            ?: player.currentMediaItem?.let { item ->
                val meta = item.mediaMetadata
                Track(
                    id = item.mediaId,
                    title = meta.title?.toString() ?: "Track",
                    artistName = meta.artist?.toString() ?: "Unknown",
                    albumName = meta.albumTitle?.toString() ?: "",
                    coverArtUrl = meta.artworkUri?.toString() ?: "",
                    durationMs = player.duration.coerceAtLeast(0L),
                    mediaUri = ""
                )
            }

        _playbackState.update {
            it.copy(
                currentTrack = currentTrack,
                isPlaying = player.isPlaying,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                queue = currentQueue,
                queueIndex = player.currentMediaItemIndex,
                shuffleMode = isSpotifyShuffleEnabled
            )
        }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    private fun applyAutoGenreEqIfEnabled(track: Track) {
        if (!SettingsManager.autoGenreEq.value) return
        val genre = MusicGenre.inferGenre(track)
        val bands = when (genre) {
            MusicGenre.MASS_BEATS, MusicGenre.HIP_HOP, MusicGenre.ELECTRONIC -> listOf(5f, 3f, 0f, 2f, 4f)
            MusicGenre.MELODIES, MusicGenre.BOLLYWOOD -> listOf(-1f, 1f, 4f, 3f, 0f)
            MusicGenre.ROCK -> listOf(4f, 2f, -1f, 3f, 5f)
            MusicGenre.LO_FI, MusicGenre.INDIE -> listOf(2f, 2f, 1f, 0f, -1f)
            MusicGenre.DEVOTIONAL -> listOf(2f, 1f, 3f, 3f, 2f)
            else -> listOf(0f, 1f, 3f, 2f, 1f)
        }
        val prefs = context.getSharedPreferences("spotify_eq_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit().putString("auto_eq_preset", "Auto (${genre.shortLabel})")
        bands.forEachIndexed { idx, v -> editor.putFloat("auto_band_$idx", v) }
        editor.apply()
        SettingsManager.notifyEqualizerChanged()
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                var pollDelayMs = 100L
                controller?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration.coerceAtLeast(0L)
                        val crossfadeSec = SettingsManager.crossfadeSeconds.value
                        if (crossfadeSec > 0 && dur > 15_000L) {
                            val halfFadeMs = (crossfadeSec * 500L).coerceAtLeast(CrossfadeCurve.MIN_OVERLAP_MS)
                            val gain = when {
                                pos < halfFadeMs -> {
                                    pollDelayMs = 40L
                                    val p = (pos.toFloat() / halfFadeMs.toFloat()).coerceIn(0f, 1f)
                                    CrossfadeCurve.incomingGain(p)
                                }
                                dur - pos < halfFadeMs -> {
                                    pollDelayMs = 40L
                                    val p = (1f - (dur - pos).toFloat() / halfFadeMs.toFloat()).coerceIn(0f, 1f)
                                    CrossfadeCurve.outgoingGain(p)
                                }
                                else -> 1.0f
                            }
                            player.volume = gain.coerceIn(0f, 1f)
                        } else if (abs(player.volume - 1.0f) > 0.01f) {
                            player.volume = 1.0f
                        }

                        _playbackState.update {
                            it.copy(
                                currentPositionMs = pos,
                                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                                durationMs = dur
                            )
                        }

                        val activeTrack = _playbackState.value.currentTrack
                        if (activeTrack != null && dur > 15_000L && pos >= (dur * 0.75).toLong() && lastCompletedTrackId != activeTrack.id) {
                            lastCompletedTrackId = activeTrack.id
                            EchoTasteMlModel.recordInteraction(activeTrack, TasteSignalType.COMPLETION)
                        }
                    } else if (abs(player.volume - 1.0f) > 0.01f) {
                        player.volume = 1.0f
                    }
                }
                delay(pollDelayMs)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        val baseQueue = queue.ifEmpty { listOf(track) }
        val clickedIdx = baseQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        originalQueueOrder = baseQueue

        val activeQueue = if (isSpotifyShuffleEnabled && baseQueue.size > 1) {
            EchoRecommendationEngine.orderUnplayedFirstShuffle(
                queue = baseQueue,
                currentIndex = clickedIdx,
                playedSongKeys = SmartRadioQueue.getRecentlyPlayedKeysSnapshot()
            )
        } else {
            baseQueue
        }
        val startIndex = activeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val generation = ++queueGenerationId
        playJob?.cancel()
        continuationJob?.cancel()

        _playbackState.update {
            it.copy(
                currentTrack = track,
                isPlaying = true,
                isLoading = true,
                queue = activeQueue,
                queueIndex = startIndex,
                shuffleMode = isSpotifyShuffleEnabled,
                errorMessage = null
            )
        }

        playJob = scope.launch(Dispatchers.IO) {
            ensureActive()
            val downloadMgr = TrackDownloadManager.getInstance(context)
            val updatedQueue = activeQueue.map { t ->
                if (downloadMgr.isTrackDownloaded(t.id)) downloadMgr.resolveTrackForPlayback(t) else t
            }.toMutableList()

            val target = updatedQueue.getOrNull(startIndex) ?: track
            val resolved = resolvePlayableTrack(target, downloadMgr)
            ensureActive()
            if (generation != queueGenerationId) return@launch

            if (startIndex in updatedQueue.indices) {
                updatedQueue[startIndex] = resolved
            }

            if (resolved.mediaUri.isBlank() || resolved.mediaUri.startsWith("saavn_enc:")) {
                withContext(Dispatchers.Main) {
                    if (generation != queueGenerationId) return@withContext
                    controller?.stop()
                    controller?.clearMediaItems()
                    currentQueue = updatedQueue
                    _playbackState.update {
                        it.copy(
                            currentTrack = resolved,
                            isPlaying = false,
                            isLoading = false,
                            queue = updatedQueue,
                            queueIndex = startIndex,
                            errorMessage = "Unable to stream this track. Check your connection."
                        )
                    }
                    EchoWidgetManager.updateAll(context, _playbackState.value)
                }
                return@launch
            }

            SmartRadioQueue.recordTrackPlayed(resolved)
            EchoRecommendationEngine.recordTrackPlay(resolved)
            try { repository.recordTrackPlayed(resolved) } catch (_: Exception) {}

            val mediaItems = updatedQueue.map { it.toMediaItem() }
            withContext(Dispatchers.Main) {
                if (generation != queueGenerationId) return@withContext
                currentQueue = updatedQueue
                val player = controller
                if (player != null) {
                    player.shuffleModeEnabled = false
                    player.setMediaItems(mediaItems, startIndex, 0L)
                    player.prepare()
                    player.play()
                } else {
                    pendingAction = {
                        controller?.let {
                            it.shuffleModeEnabled = false
                            it.setMediaItems(mediaItems, startIndex, 0L)
                            it.prepare()
                            it.play()
                        }
                    }
                }

                _playbackState.update {
                    it.copy(
                        currentTrack = resolved,
                        isPlaying = true,
                        isLoading = player == null || player.playbackState == Player.STATE_BUFFERING,
                        queue = updatedQueue,
                        queueIndex = startIndex,
                        shuffleMode = isSpotifyShuffleEnabled,
                        errorMessage = null
                    )
                }
                EchoWidgetManager.updateAll(context, _playbackState.value)
                startProgressPolling()
            }

            // Pre-resolve the next track in queue for smooth transitions
            val nextIndex = startIndex + 1
            if (nextIndex < updatedQueue.size) {
                val nextTrack = updatedQueue[nextIndex]
                val nextResolved = resolvePlayableTrack(nextTrack, downloadMgr)
                if (nextResolved.mediaUri != nextTrack.mediaUri && nextResolved.mediaUri.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        if (generation == queueGenerationId && nextIndex in currentQueue.indices) {
                            val mutable = currentQueue.toMutableList()
                            mutable[nextIndex] = nextResolved
                            currentQueue = mutable
                            controller?.replaceMediaItem(nextIndex, nextResolved.toMediaItem())
                        }
                    }
                }
            }
        }
    }

    /**
     * Starts an immediate Same-Genre Radio queue seeded by [seedTrack] and ordered with
     * Spotify's balanced artist-spacing interleave, then enriches asynchronously online.
     */
    fun playSameGenreRadio(seedTrack: Track, genreOverride: MusicGenre? = null) {
        val targetGenre = genreOverride ?: getEffectiveGenre(seedTrack).let {
            if (it == MusicGenre.ALL) MusicGenre.inferGenre(seedTrack) else it
        }
        setGenreLock(targetGenre)
        val initialQueue = EchoRecommendationEngine.buildSameGenreOrderedQueue(
            seedTrack = seedTrack,
            candidates = SampleCatalog.sampleTracks,
            targetGenre = targetGenre,
            limit = 16
        )
        playTrack(seedTrack, initialQueue)

        // Enrich remaining queue with live online same-genre tracks
        val genAtStart = queueGenerationId
        scope.launch(Dispatchers.IO) {
            val live = SmartRadioQueue.generateRadioContinuationAsync(
                seedTrack = seedTrack,
                currentQueue = initialQueue,
                repository = repository,
                lockedGenre = targetGenre,
                autoMatchGenre = true
            )
            if (live.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                if (genAtStart != queueGenerationId) return@withContext
                val existingIds = currentQueue.mapTo(HashSet()) { it.id }
                val unique = live.filter { existingIds.add(it.id) }
                if (unique.isNotEmpty()) {
                    currentQueue = currentQueue + unique
                    originalQueueOrder = originalQueueOrder + unique
                    unique.forEach { controller?.addMediaItem(it.toMediaItem()) }
                    updateState()
                }
            }
        }
    }

    /**
     * Launches an endless Same-Genre Radio station for any [MusicGenre].
     */
    fun playGenreRadio(genre: MusicGenre) {
        setGenreLock(genre)
        scope.launch(Dispatchers.IO) {
            val query = genre.searchQueries.firstOrNull() ?: "top hits"
            val online = if (genre == MusicGenre.ALL) {
                runCatching { repository.getDailyTop100() }.getOrDefault(emptyList())
            } else {
                runCatching { repository.search(query) }.getOrDefault(emptyList())
            }
            val pool = (online + SampleCatalog.sampleTracks)
                .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
            val matching = if (genre == MusicGenre.ALL) {
                pool
            } else {
                pool.filter { genre.matchesTrack(it) || MusicGenre.inferGenre(it) == genre }
                    .ifEmpty { pool }
            }
            val seed = matching.firstOrNull() ?: SampleCatalog.sampleTracks.firstOrNull() ?: return@launch
            val orderedQueue = EchoRecommendationEngine.buildSameGenreOrderedQueue(
                seedTrack = seed,
                candidates = matching,
                targetGenre = genre,
                limit = 20
            )
            withContext(Dispatchers.Main) {
                playTrack(seed, orderedQueue)
            }
        }
    }

    fun setGenreLock(genre: MusicGenre) {
        _activeGenreLock.value = genre
        SettingsManager.setGenreLock(genre)
        if (genre != MusicGenre.ALL) {
            // Re-align upcoming continuation if queue is low
            _playbackState.value.currentTrack?.let { maybeAppendSmartRadio(it, queueGenerationId) }
        }
    }

    fun setAutoMatchGenre(enabled: Boolean) {
        _autoMatchGenre.value = enabled
        SettingsManager.setAutoMatchGenre(enabled)
        if (enabled) {
            setGenreLock(MusicGenre.ALL)
        }
    }

    fun lockGenreToCurrentTrack() {
        val current = _playbackState.value.currentTrack ?: return
        val inferred = MusicGenre.inferGenre(current)
        setGenreLock(inferred)
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        val player = controller
        if (player == null) {
            state.currentTrack?.let { playTrack(it, currentQueue.ifEmpty { listOf(it) }) }
            return
        }
        if (state.errorMessage != null || player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE) {
            val current = state.currentTrack
            if (current != null) {
                playTrack(current, currentQueue.ifEmpty { listOf(current) })
                return
            }
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    fun skipNext() {
        val player = controller ?: return
        val currentTrack = _playbackState.value.currentTrack
        val pos = player.currentPosition
        if (currentTrack != null && pos in 1L..24_999L) {
            EchoRecommendationEngine.recordEarlySkip(currentTrack)
        }
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            val nextIdx = (_playbackState.value.queueIndex + 1)
            if (nextIdx in currentQueue.indices) {
                playTrack(currentQueue[nextIdx], currentQueue)
            }
        }
    }

    fun skipPrevious() {
        val player = controller ?: return
        if (player.currentPosition > 3000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    /**
     * Spotify-style Shuffle Toggle:
     * Applies Unplayed-First + Balanced Artist-Spacing Shuffle to the upcoming queue
     * without interrupting the currently playing audio stream.
     */
    fun toggleShuffle() {
        isSpotifyShuffleEnabled = !isSpotifyShuffleEnabled
        val player = controller
        val currentIdx = player?.currentMediaItemIndex ?: _playbackState.value.queueIndex
        val activeTrack = currentQueue.getOrNull(currentIdx) ?: _playbackState.value.currentTrack

        if (currentQueue.size > 1 && activeTrack != null) {
            val reordered = if (isSpotifyShuffleEnabled) {
                EchoRecommendationEngine.orderUnplayedFirstShuffle(
                    queue = currentQueue,
                    currentIndex = currentIdx.coerceIn(0, currentQueue.lastIndex),
                    playedSongKeys = SmartRadioQueue.getRecentlyPlayedKeysSnapshot()
                )
            } else {
                // Restore original playlist order for remaining tracks after activeTrack
                val base = originalQueueOrder.ifEmpty { currentQueue }
                val baseActiveIdx = base.indexOfFirst { it.id == activeTrack.id }
                if (baseActiveIdx != -1) {
                    val tail = base.subList(baseActiveIdx + 1, base.size)
                    val head = base.subList(0, baseActiveIdx)
                    listOf(activeTrack) + tail + head
                } else {
                    currentQueue
                }
            }

            if (player != null && currentIdx in 0 until player.mediaItemCount) {
                // Remove items after & before currentIndex without touching the playing item
                if (currentIdx + 1 < player.mediaItemCount) {
                    player.removeMediaItems(currentIdx + 1, player.mediaItemCount)
                }
                if (currentIdx > 0) {
                    player.removeMediaItems(0, currentIdx)
                }
                val upcoming = reordered.drop(1)
                if (upcoming.isNotEmpty()) {
                    player.addMediaItems(upcoming.map { it.toMediaItem() })
                }
            }
            currentQueue = reordered
        }

        _playbackState.update {
            it.copy(
                shuffleMode = isSpotifyShuffleEnabled,
                queue = currentQueue,
                queueIndex = controller?.currentMediaItemIndex ?: 0
            )
        }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFavorite(track: Track) {
        scope.launch(Dispatchers.IO) {
            val newStatus = repository.toggleFavorite(track)
            EchoTasteMlModel.recordInteraction(
                track = track.copy(isFavorite = newStatus),
                signal = if (newStatus) TasteSignalType.LIKE else TasteSignalType.UNLIKE
            )
            withContext(Dispatchers.Main) {
                _playbackState.update { state ->
                    val updatedCurrent = if (state.currentTrack?.id == track.id) {
                        state.currentTrack.copy(isFavorite = newStatus)
                    } else state.currentTrack
                    val updatedQueue = state.queue.map {
                        if (it.id == track.id) it.copy(isFavorite = newStatus) else it
                    }
                    currentQueue = currentQueue.map {
                        if (it.id == track.id) it.copy(isFavorite = newStatus) else it
                    }
                    originalQueueOrder = originalQueueOrder.map {
                        if (it.id == track.id) it.copy(isFavorite = newStatus) else it
                    }
                    state.copy(currentTrack = updatedCurrent, queue = updatedQueue)
                }
            }
        }
    }

    /**
     * Splices [track] directly after the currently playing track (Spotify/LastWave Play Next).
     */
    fun playNext(track: Track) {
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val currentIdx = (controller?.currentMediaItemIndex ?: _playbackState.value.queueIndex)
            .coerceIn(0, currentQueue.lastIndex)
        val insertPos = currentIdx + 1

        // Remove duplicate upcoming entry in player if present
        val existingUpcomingIdx = currentQueue.indexOfFirst { it.id == track.id }
        if (existingUpcomingIdx > currentIdx) {
            controller?.removeMediaItem(existingUpcomingIdx)
        }

        currentQueue = EchoRecommendationEngine.insertPlayNext(currentQueue, currentIdx, track)
        originalQueueOrder = EchoRecommendationEngine.insertPlayNext(originalQueueOrder, currentIdx, track)
        controller?.addMediaItem(insertPos, track.toMediaItem())
        _playbackState.update {
            it.copy(queue = currentQueue, queueIndex = controller?.currentMediaItemIndex ?: currentIdx)
        }
    }

    fun addToQueue(track: Track) {
        currentQueue = currentQueue + track
        originalQueueOrder = originalQueueOrder + track
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

    /**
     * Clears all upcoming tracks after the currently playing item without interrupting playback.
     */
    fun clearUpcomingQueue() {
        val player = controller
        val currentIdx = (player?.currentMediaItemIndex ?: _playbackState.value.queueIndex)
            .coerceIn(0, (currentQueue.size - 1).coerceAtLeast(0))
        if (currentQueue.size <= currentIdx + 1) return

        val kept = currentQueue.subList(0, currentIdx + 1).toList()
        currentQueue = kept
        originalQueueOrder = kept
        if (player != null && currentIdx + 1 < player.mediaItemCount) {
            player.removeMediaItems(currentIdx + 1, player.mediaItemCount)
        }
        _playbackState.update { it.copy(queue = currentQueue, queueIndex = currentIdx) }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    /**
     * Re-orders upcoming tracks with Spotify's Unplayed-First + Artist-Balanced shuffle
     * while keeping the active track playing seamlessly.
     */
    fun shuffleUpcomingQueue() {
        val player = controller
        val currentIdx = (player?.currentMediaItemIndex ?: _playbackState.value.queueIndex)
            .coerceIn(0, (currentQueue.size - 1).coerceAtLeast(0))
        if (currentQueue.size <= currentIdx + 2) return

        val playedOrCurrent = currentQueue.subList(0, currentIdx + 1)
        val upcoming = currentQueue.subList(currentIdx + 1, currentQueue.size)
        val shuffledUpcoming = EchoRecommendationEngine.orderUnplayedFirstShuffle(
            queue = upcoming,
            currentIndex = 0,
            playedSongKeys = SmartRadioQueue.getRecentlyPlayedKeysSnapshot()
        )
        val newQueue = playedOrCurrent + shuffledUpcoming
        currentQueue = newQueue
        if (player != null && currentIdx + 1 < player.mediaItemCount) {
            player.removeMediaItems(currentIdx + 1, player.mediaItemCount)
            player.addMediaItems(shuffledUpcoming.map { it.toMediaItem() })
        }
        _playbackState.update { it.copy(queue = currentQueue, queueIndex = currentIdx) }
        EchoWidgetManager.updateAll(context, _playbackState.value)
    }

    fun playQueueIndex(index: Int) {
        if (index !in currentQueue.indices) return
        controller?.seekTo(index, 0L)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerEndOfTrack = false
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (remaining <= 0) {
                controller?.pause()
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        sleepTimerEndOfTrack = true
        val state = _playbackState.value
        val initialRem = if (state.durationMs > 0L) {
            ((state.durationMs - state.currentPositionMs) / 1000L).coerceAtLeast(1L)
        } else null
        _sleepTimerRemainingSeconds.value = initialRem
        sleepTimerJob = scope.launch {
            while (isActive && sleepTimerEndOfTrack) {
                delay(1000)
                val s = _playbackState.value
                if (s.durationMs <= 0L) {
                    _sleepTimerRemainingSeconds.value = null
                    continue
                }
                val rem = ((s.durationMs - s.currentPositionMs) / 1000L).coerceAtLeast(0L)
                _sleepTimerRemainingSeconds.value = rem
                if (rem <= 1L && s.isPlaying) {
                    delay(1200)
                    if (sleepTimerEndOfTrack) {
                        controller?.pause()
                        cancelSleepTimer()
                    }
                    break
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerEndOfTrack = false
        _sleepTimerRemainingSeconds.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        SettingsManager.setPlaybackSpeed(clamped)
        controller?.setPlaybackSpeed(clamped)
    }

    fun setSmartRadioEnabled(enabled: Boolean) {
        _isSmartRadioEnabled.value = enabled
        SettingsManager.setSmartRadioEnabled(enabled)
    }

    fun playFirstTrack() {
        val tracks = SampleCatalog.sampleTracks
        if (tracks.isNotEmpty()) {
            playTrack(tracks.first(), tracks)
        }
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
            !mediaUri.contains("googleapis.com")
        ) {
            mediaUri
        } else {
            ""
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .apply {
                if (safeUri.isNotBlank()) {
                    setUri(safeUri)
                } else {
                    setUri(Uri.EMPTY)
                }
            }
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
