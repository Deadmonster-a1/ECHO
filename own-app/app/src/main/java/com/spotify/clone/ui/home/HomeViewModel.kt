package com.spotify.clone.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.mvi.UiEvent
import com.spotify.clone.core.mvi.UiState
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.recommendation.DailyFeedCoordinator
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.recommendation.EchoTasteMlModel
import com.spotify.clone.core.recommendation.MlTrackPrediction
import com.spotify.clone.core.recommendation.SongUpgradeEngine
import com.spotify.clone.core.recommendation.SongUpgradeStats
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class HomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val dailyDateLabel: String = DailyFeedCoordinator.friendlyDateLabel(),
    val lastUpdatedDateKey: String = DailyFeedCoordinator.formatDateKey(),
    val editorialBanners: List<EditorialBanner> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val favoriteTracks: List<Track> = emptyList(),
    val recommendedItems: List<MlTrackPrediction> = emptyList(),
    val recommendedTracks: List<Track> = emptyList(),
    val recommendedSubtitle: String = "",
    val newSongsToday: List<Track> = emptyList(),
    val likedAnchorSubtitle: String = "",
    val likedInspiredTracks: List<Track> = emptyList(),
    val genreHubs: List<Playlist> = emptyList(),
    val moodMixes: List<Playlist> = emptyList(),
    val exploreGems: List<MlTrackPrediction> = emptyList(),
    val timeFlowTitle: String = EchoRecommendationEngine.getTimeOfDayBucket().label,
    val timeFlowTracks: List<Track> = emptyList(),
    val quickPicks: List<Playlist> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList(),
    val newAlbums: List<Playlist> = emptyList(),
    val trendingTracks: List<Track> = emptyList(),
    val upgradedTracks: List<Track> = emptyList(),
    val upgradeStats: SongUpgradeStats = SongUpgradeStats(),
    val artists: List<Artist> = emptyList()
) : UiState

sealed interface HomeEvent : UiEvent {
    data object Refresh : HomeEvent
    data object CheckDailyRollover : HomeEvent
    data object RunSongUpgrade : HomeEvent
    data class PlayTrack(val track: Track, val queue: List<Track>) : HomeEvent
    data class PlaySameGenreRadio(val track: Track) : HomeEvent
    data class PlayGenreRadio(val genre: MusicGenre) : HomeEvent
    data class PlayPlaylist(val playlist: Playlist) : HomeEvent
    data class StartArtistRadio(val artist: Artist) : HomeEvent
    data class PlayHero(val banner: EditorialBanner) : HomeEvent
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private val musicController = MusicController.getInstance(application)

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private var upgradeJob: Job? = null
    private var midnightJob: Job? = null
    private var lastCatalogPool: List<Track> = emptyList()
    private var lastRawPlaylists: List<Playlist> = emptyList()

    companion object {
        @Volatile
        private var cachedHomeState: HomeState? = null
        @Volatile
        private var cachedDateKey: String? = null
    }

    init {
        EchoRecommendationEngine.init(application)
        observeTasteStreams()
        observeUpgradeStats()
        scheduleMidnightRollover()

        val todayKey = DailyFeedCoordinator.formatDateKey()
        val cached = cachedHomeState
        if (cached != null && cachedDateKey == todayKey && !DailyFeedCoordinator.needsDailyRefresh()) {
            _uiState.value = cached.copy(isLoading = false)
            loadHomeData(backgroundRefresh = true, forceRefresh = false)
        } else {
            loadHomeData(backgroundRefresh = false, forceRefresh = true)
        }
    }

    private fun observeTasteStreams() {
        viewModelScope.launch {
            combine(
                repository.getRecentlyPlayedTracks(),
                repository.getFavoriteTracks()
            ) { recent, favorites ->
                recent to favorites
            }.collect { (recent, favorites) ->
                val upgradedRecent = SongUpgradeEngine.upgradeCatalogFast(recent)
                val upgradedFavs = SongUpgradeEngine.upgradeCatalogFast(favorites)
                EchoTasteMlModel.syncLikedCoOccurrence(upgradedFavs)

                _uiState.update { current ->
                    val favIds = upgradedFavs.mapTo(HashSet()) { it.id }
                    val syncFav: (Track) -> Track = { t ->
                        val isFav = t.id in favIds
                        if (t.isFavorite == isFav) t else t.copy(isFavorite = isFav)
                    }
                    val pool = (lastCatalogPool + current.newSongsToday + current.trendingTracks +
                        current.featuredPlaylists.flatMap { it.tracks }).distinctBy { it.id }.map(syncFav)
                    val recsRaw = EchoRecommendationEngine.generateHomeRecommendationsWithReasons(
                        recentlyPlayed = upgradedRecent,
                        favorites = upgradedFavs,
                        catalogPool = pool,
                        limit = 36
                    )
                    val (likedLabel, likedRaw) = EchoRecommendationEngine.generateLikedInspiredMix(
                        favorites = upgradedFavs,
                        catalogPool = pool,
                        limit = 30
                    )
                    val genreHubs = EchoRecommendationEngine.generateGenreMixes(
                        favorites = upgradedFavs,
                        recentlyPlayed = upgradedRecent,
                        catalogPool = pool,
                        limit = 10
                    )
                    val dailyMixes = EchoRecommendationEngine.generateDailyMixes(
                        recentlyPlayed = upgradedRecent,
                        favorites = upgradedFavs,
                        catalogPool = pool
                    )
                    val moodMixes = buildMoodClusters(pool, upgradedFavs, upgradedRecent)
                    val exploreRaw = buildExploreGems(pool, upgradedFavs, upgradedRecent, recsRaw.map { it.track })
                    val timeBucket = EchoRecommendationEngine.getTimeOfDayBucket()
                    val timeFlowRaw = buildTimeFlowTracks(pool, upgradedFavs, upgradedRecent, timeBucket)
                    val mergedPlaylists = (dailyMixes + lastRawPlaylists).distinctBy { it.id }
                    AudioRepository.registerGeneratedPlaylists(dailyMixes + genreHubs + moodMixes)

                    val deduped = deduplicateHomeShelves(
                        recent = upgradedRecent,
                        recs = recsRaw,
                        newSongs = current.newSongsToday.map(syncFav),
                        timeFlow = timeFlowRaw,
                        liked = likedRaw,
                        explore = exploreRaw,
                        trending = current.trendingTracks.map(syncFav)
                    )

                    val preferredGenre = EchoRecommendationEngine.getTopPreferredGenre(upgradedRecent, upgradedFavs)
                    val timeLabel = timeBucket.label
                    val seededArtists = EchoRecommendationEngine.getOnboardedArtists()
                    val subtitle = if (seededArtists.isNotEmpty() && upgradedRecent.isEmpty() && upgradedFavs.isEmpty()) {
                        "${preferredGenre.displayName} • Tuned for you"
                    } else {
                        "${preferredGenre.displayName} • $timeLabel"
                    }
                    val quickPicks = buildQuickPicks(upgradedFavs, dailyMixes, lastRawPlaylists)
                    val banners = EchoRecommendationEngine.generateDailySpotlightBanners(
                        favorites = upgradedFavs,
                        newSongs = deduped.newSongs,
                        dailyMixes = dailyMixes
                    )

                    current.copy(
                        recentlyPlayed = deduped.recent,
                        favoriteTracks = upgradedFavs,
                        newSongsToday = deduped.newSongs,
                        trendingTracks = deduped.trending,
                        upgradedTracks = current.upgradedTracks.map(syncFav),
                        recommendedItems = deduped.recs,
                        recommendedTracks = deduped.recs.map { it.track },
                        recommendedSubtitle = subtitle,
                        likedAnchorSubtitle = likedLabel,
                        likedInspiredTracks = deduped.liked,
                        genreHubs = genreHubs,
                        moodMixes = moodMixes,
                        exploreGems = deduped.explore,
                        timeFlowTitle = timeLabel,
                        timeFlowTracks = deduped.timeFlow,
                        quickPicks = quickPicks,
                        featuredPlaylists = mergedPlaylists,
                        editorialBanners = banners.ifEmpty { current.editorialBanners }
                    )
                }
            }
        }
    }

    private fun observeUpgradeStats() {
        viewModelScope.launch {
            SongUpgradeEngine.upgradeStats.collect { stats ->
                _uiState.update { it.copy(upgradeStats = stats) }
            }
        }
    }

    private fun scheduleMidnightRollover() {
        midnightJob?.cancel()
        midnightJob = viewModelScope.launch {
            while (isActive) {
                val waitMs = DailyFeedCoordinator.msUntilNextMidnight()
                delay(waitMs)
                if (DailyFeedCoordinator.checkDayRollover()) {
                    loadHomeData(backgroundRefresh = true, forceRefresh = true)
                }
            }
        }
    }

    private fun buildMoodClusters(
        pool: List<Track>,
        favorites: List<Track>,
        recent: List<Track>
    ): List<Playlist> {
        val fullPool = SongUpgradeEngine.upgradeCatalogFast(pool + SampleCatalog.sampleTracks)
        return EchoTasteMlModel.clusterTracksForDailyMixes(
            pool = fullPool,
            favorites = favorites,
            recentlyPlayed = recent,
            k = 4
        ).mapIndexed { idx, cluster ->
            Playlist(
                id = "mood_cluster_${idx + 1}_${cluster.dominantGenre.id}",
                name = "${cluster.dominantGenre.shortLabel} Flow",
                description = "${cluster.dominantGenre.displayName} • ${cluster.tracks.size} tracks",
                coverArtUrl = cluster.tracks.first().coverArtUrl,
                ownerName = "Mood Mix",
                tracks = cluster.tracks
            )
        }
    }

    private fun buildExploreGems(
        pool: List<Track>,
        favorites: List<Track>,
        recent: List<Track>,
        alreadyRecommended: List<Track>,
        dailySeed: Long = DailyFeedCoordinator.dailySeed()
    ): List<MlTrackPrediction> {
        val excludeKeys = (alreadyRecommended + recent).mapTo(HashSet()) { homeSongKey(it) }
        val candidates = SongUpgradeEngine.upgradeCatalogFast(pool + SampleCatalog.sampleTracks)
            .filter { homeSongKey(it) !in excludeKeys && !EchoRecommendationEngine.isJunkTitle(it.title) }
            .ifEmpty { SampleCatalog.sampleTracks }
        return EchoTasteMlModel.rankCandidatesWithMl(
            candidates = candidates,
            favorites = favorites,
            recentlyPlayed = recent,
            limit = 32,
            explorationAlpha = 0.85f,
            lambdaMmr = 0.62f,
            dailySeed = dailySeed + 7919L
        )
    }

    private fun buildTimeFlowTracks(
        pool: List<Track>,
        favorites: List<Track>,
        recent: List<Track>,
        timeBucket: com.spotify.clone.core.recommendation.TimeOfDayBucket
    ): List<Track> {
        val favIds = favorites.mapTo(HashSet()) { it.id }
        val recentIds = recent.mapTo(HashSet()) { it.id }
        val fullPool = SongUpgradeEngine.upgradeCatalogFast(pool + SampleCatalog.sampleTracks)
            .distinctBy { homeSongKey(it) }
        val scored = fullPool.map { track ->
            val score = EchoRecommendationEngine.scoreLocalTasteSignal(
                track = track,
                playCount = if (track.id in recentIds) 2 else 1,
                skipCount = 0,
                isLiked = track.id in favIds,
                isRecent = track.id in recentIds,
                timeBucket = timeBucket
            )
            track to score
        }.sortedByDescending { it.second }.map { it.first }
        return EchoRecommendationEngine.spotifyArtistInterleave(scored.take(40))
    }

    private data class DedupedHomeShelves(
        val recent: List<Track>,
        val recs: List<MlTrackPrediction>,
        val newSongs: List<Track>,
        val timeFlow: List<Track>,
        val liked: List<Track>,
        val explore: List<MlTrackPrediction>,
        val trending: List<Track>
    )

    private fun deduplicateHomeShelves(
        recent: List<Track>,
        recs: List<MlTrackPrediction>,
        newSongs: List<Track>,
        timeFlow: List<Track>,
        liked: List<Track>,
        explore: List<MlTrackPrediction>,
        trending: List<Track>
    ): DedupedHomeShelves {
        val totalDistinct = (recent + recs.map { it.track } + newSongs + timeFlow + liked +
            explore.map { it.track } + trending)
            .distinctBy { homeSongKey(it) }
            .size
        val perShelfCap = when {
            totalDistinct >= 70 -> 12
            totalDistinct >= 45 -> 8
            else -> 6
        }

        val seenKeys = HashSet<String>()
        val seenIds = HashSet<String>()

        fun accept(track: Track): Boolean {
            val key = homeSongKey(track)
            if (track.id in seenIds || key in seenKeys) return false
            seenIds.add(track.id)
            seenKeys.add(key)
            return true
        }

        val uniqueRecent = recent.filter { accept(it) }.take(perShelfCap)
        val uniqueRecs = recs.filter { accept(it.track) }.take(perShelfCap)
        val uniqueNew = newSongs.filter { accept(it) }.take(perShelfCap)
        val uniqueTimeFlow = timeFlow.filter { accept(it) }.take(perShelfCap)
        val uniqueLiked = liked.filter { accept(it) }.take(perShelfCap)
        val uniqueExplore = explore.filter { accept(it.track) }.take(perShelfCap)
        val uniqueTrending = trending.filter { accept(it) }.take(perShelfCap)

        return DedupedHomeShelves(
            recent = uniqueRecent,
            recs = uniqueRecs,
            newSongs = uniqueNew,
            timeFlow = uniqueTimeFlow,
            liked = uniqueLiked,
            explore = uniqueExplore,
            trending = uniqueTrending
        )
    }

    private fun homeSongKey(track: Track): String {
        val cleanTitle = track.title.trim().lowercase(Locale.ROOT)
            .replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("(?i)\\s*-\\s*(?:from|remastered|single|live|acoustic).*$"), "")
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val cleanArtist = EchoRecommendationEngine.primaryArtistOf(track.artistName)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return "$cleanTitle|$cleanArtist"
    }

    private fun buildQuickPicks(
        favorites: List<Track>,
        dailyMixes: List<Playlist>,
        rawPlaylists: List<Playlist>
    ): List<Playlist> {
        val likedTile = if (favorites.isNotEmpty()) {
            Playlist(
                id = "pl_liked_songs",
                name = "Liked Songs (${favorites.size})",
                description = "Your favorite tracks",
                coverArtUrl = favorites.first().coverArtUrl,
                ownerName = "You",
                tracks = favorites
            )
        } else null
        val discoverTile = dailyMixes.firstOrNull { it.id.startsWith("daily_mix_discover_") }
        val otherMixes = dailyMixes.filter { it.id != discoverTile?.id }
        return (listOfNotNull(likedTile, discoverTile) + otherMixes + rawPlaylists)
            .distinctBy { it.id }
            .take(6)
    }

    private fun loadHomeData(backgroundRefresh: Boolean, forceRefresh: Boolean) {
        viewModelScope.launch {
            val todayKey = DailyFeedCoordinator.formatDateKey()
            AudioRepository.invalidateDailyCacheIfNewDay(todayKey)
            if (forceRefresh) {
                AudioRepository.forceInvalidateHomeCache()
            }

            if (!backgroundRefresh && _uiState.value.featuredPlaylists.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else if (!backgroundRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            }

            try {
                val recent = _uiState.value.recentlyPlayed.ifEmpty {
                    runCatching { repository.getRecentlyPlayedTracks().first() }.getOrDefault(emptyList())
                }
                val favorites = _uiState.value.favoriteTracks.ifEmpty {
                    runCatching { repository.getFavoriteTracks().first() }.getOrDefault(emptyList())
                }
                val rankedGenres = EchoTasteMlModel.getRankedPreferredGenres(favorites, recent)
                val preferredLangs = EchoRecommendationEngine.getOnboardedLanguages()

                val playlistsDeferred = async {
                    withTimeoutOrNull(3800) { repository.getHomePlaylists() }
                        ?: SampleCatalog.samplePlaylists.filter { it.id != "pl_liked_songs" }
                }
                val artistsDeferred = async {
                    withTimeoutOrNull(3200) { repository.getHomeArtists() }
                        ?: SampleCatalog.sampleArtists
                }
                val newSongsDeferred = async {
                    withTimeoutOrNull(4200) { repository.getDailyNewSongsFeed(preferredLangs, rankedGenres) }
                        ?: emptyList()
                }
                val topTracksDeferred = async {
                    withTimeoutOrNull(3800) { repository.getDailyTop100() } ?: emptyList()
                }
                val newAlbumsDeferred = async {
                    withTimeoutOrNull(3800) { repository.getNewAlbums() } ?: emptyList()
                }

                val playlists = playlistsDeferred.await()
                val artists = artistsDeferred.await()
                val rawNewSongs = newSongsDeferred.await()
                val topTracks = topTracksDeferred.await().ifEmpty { SampleCatalog.sampleTracks }
                val newAlbums = newAlbumsDeferred.await()

                lastRawPlaylists = playlists
                val combinedPool = SongUpgradeEngine.upgradeCatalogFast(
                    rawNewSongs + topTracks + playlists.flatMap { it.tracks } + newAlbums.flatMap { it.tracks } + SampleCatalog.sampleTracks
                ).distinctBy { homeSongKey(it) }
                lastCatalogPool = combinedPool

                // Rank daily new songs through the AI/ML taste model with daily seed rotation
                val dailySeed = DailyFeedCoordinator.dailySeed(todayKey)
                val mlNewSongsRaw = EchoTasteMlModel.rankCandidatesWithMl(
                    candidates = (rawNewSongs.ifEmpty { topTracks }),
                    favorites = favorites,
                    recentlyPlayed = recent,
                    limit = 34,
                    dailySeed = dailySeed
                ).map { it.track }

                val recsRaw = EchoRecommendationEngine.generateHomeRecommendationsWithReasons(
                    recentlyPlayed = recent,
                    favorites = favorites,
                    catalogPool = combinedPool,
                    limit = 36,
                    dailySeed = dailySeed
                )
                val (likedLabel, likedRaw) = EchoRecommendationEngine.generateLikedInspiredMix(
                    favorites = favorites,
                    catalogPool = combinedPool,
                    limit = 30
                )
                val dailyMixes = EchoRecommendationEngine.generateDailyMixes(
                    recentlyPlayed = recent,
                    favorites = favorites,
                    catalogPool = combinedPool,
                    dailySeed = dailySeed
                )
                val genreHubs = EchoRecommendationEngine.generateGenreMixes(
                    favorites = favorites,
                    recentlyPlayed = recent,
                    catalogPool = combinedPool,
                    limit = 10
                )
                val moodMixes = buildMoodClusters(combinedPool, favorites, recent)
                val exploreRaw = buildExploreGems(combinedPool, favorites, recent, recsRaw.map { it.track }, dailySeed)
                val timeBucket = EchoRecommendationEngine.getTimeOfDayBucket()
                val timeFlowRaw = buildTimeFlowTracks(combinedPool, favorites, recent, timeBucket)
                val trendingRaw = SongUpgradeEngine.upgradeCatalogFast(topTracks)
                AudioRepository.registerGeneratedPlaylists(dailyMixes + genreHubs + moodMixes)

                val deduped = deduplicateHomeShelves(
                    recent = recent,
                    recs = recsRaw,
                    newSongs = mlNewSongsRaw,
                    timeFlow = timeFlowRaw,
                    liked = likedRaw,
                    explore = exploreRaw,
                    trending = trendingRaw
                )

                val banners = (EchoRecommendationEngine.generateDailySpotlightBanners(
                    favorites = favorites,
                    newSongs = deduped.newSongs,
                    dailyMixes = dailyMixes
                ) + repository.getEditorialBanners()).distinctBy { it.title.lowercase() }

                val mergedPlaylists = (dailyMixes + playlists).distinctBy { it.id }
                val quickPicks = buildQuickPicks(favorites, dailyMixes, playlists)
                val preferredGenre = EchoRecommendationEngine.getTopPreferredGenre(recent, favorites)

                DailyFeedCoordinator.markDailyRefreshed()

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        dailyDateLabel = DailyFeedCoordinator.friendlyDateLabel(),
                        lastUpdatedDateKey = todayKey,
                        editorialBanners = banners,
                        recommendedItems = deduped.recs,
                        recommendedTracks = deduped.recs.map { it.track },
                        recommendedSubtitle = "${preferredGenre.displayName} • ${timeBucket.label}",
                        newSongsToday = deduped.newSongs,
                        likedAnchorSubtitle = likedLabel,
                        likedInspiredTracks = deduped.liked,
                        genreHubs = genreHubs,
                        moodMixes = moodMixes,
                        exploreGems = deduped.explore,
                        timeFlowTitle = timeBucket.label,
                        timeFlowTracks = deduped.timeFlow,
                        quickPicks = quickPicks,
                        featuredPlaylists = mergedPlaylists,
                        trendingTracks = deduped.trending,
                        newAlbums = newAlbums.ifEmpty { playlists.drop(2) },
                        artists = artists
                    )
                }
                cachedHomeState = _uiState.value
                cachedDateKey = todayKey

                // Stage 2: Automatic Background Song Upgrade Pass (320kbps Studio + HD metadata + Room DB)
                triggerBackgroundSongUpgrade(
                    candidates = deduped.newSongs + favorites + deduped.recent + deduped.recs.map { it.track }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (it.featuredPlaylists.isEmpty()) "Could not load catalog. Tap to retry." else null
                    )
                }
            }
        }
    }

    private fun triggerBackgroundSongUpgrade(candidates: List<Track>) {
        if (upgradeJob?.isActive == true) return
        upgradeJob = viewModelScope.launch {
            val persisted = runCatching { repository.getAllPersistedTracks() }.getOrDefault(emptyList())
            val toUpgrade = (candidates + persisted).distinctBy { it.id }
            val newlyUpgraded = SongUpgradeEngine.upgradeTracksDeep(
                tracks = toUpgrade,
                maxConcurrentBatch = 10,
                onTrackUpgraded = { upgraded ->
                    runCatching { repository.persistUpgradedTrack(upgraded) }
                }
            )
            if (newlyUpgraded.isNotEmpty()) {
                val upgradedMap = newlyUpgraded.associateBy { it.id }
                _uiState.update { current ->
                    val replaceIfUpgraded: (Track) -> Track = { t -> upgradedMap[t.id] ?: t }
                    current.copy(
                        newSongsToday = current.newSongsToday.map(replaceIfUpgraded),
                        recommendedItems = current.recommendedItems.map { pred ->
                            pred.copy(track = replaceIfUpgraded(pred.track))
                        },
                        recommendedTracks = current.recommendedTracks.map(replaceIfUpgraded),
                        likedInspiredTracks = current.likedInspiredTracks.map(replaceIfUpgraded),
                        timeFlowTracks = current.timeFlowTracks.map(replaceIfUpgraded),
                        exploreGems = current.exploreGems.map { pred ->
                            pred.copy(track = replaceIfUpgraded(pred.track))
                        },
                        trendingTracks = current.trendingTracks.map(replaceIfUpgraded),
                        upgradedTracks = (newlyUpgraded + current.upgradedTracks).distinctBy { it.id }.take(12)
                    )
                }
                cachedHomeState = _uiState.value
            }
        }
    }

    fun dispatch(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> loadHomeData(backgroundRefresh = false, forceRefresh = true)
            is HomeEvent.CheckDailyRollover -> {
                if (DailyFeedCoordinator.checkDayRollover()) {
                    loadHomeData(backgroundRefresh = true, forceRefresh = true)
                }
            }
            is HomeEvent.RunSongUpgrade -> {
                val current = _uiState.value
                val pool = current.newSongsToday + current.favoriteTracks + current.recentlyPlayed + current.trendingTracks
                upgradeJob?.cancel()
                triggerBackgroundSongUpgrade(pool)
            }
            is HomeEvent.PlayTrack -> musicController.playTrack(event.track, event.queue)
            is HomeEvent.PlaySameGenreRadio -> musicController.playSameGenreRadio(event.track)
            is HomeEvent.PlayGenreRadio -> musicController.playGenreRadio(event.genre)
            is HomeEvent.PlayPlaylist -> {
                if (event.playlist.tracks.isNotEmpty()) {
                    musicController.playTrack(event.playlist.tracks.first(), event.playlist.tracks)
                } else {
                    viewModelScope.launch {
                        val resolved = repository.getPlaylistById(event.playlist.id)
                        val tracks = resolved?.tracks?.takeIf { it.isNotEmpty() }
                            ?: repository.search(event.playlist.name)
                        if (tracks.isNotEmpty()) {
                            musicController.playTrack(tracks.first(), tracks)
                        }
                    }
                }
            }
            is HomeEvent.StartArtistRadio -> {
                viewModelScope.launch {
                    val tracks = repository.search(event.artist.name)
                    if (tracks.isNotEmpty()) {
                        musicController.playSameGenreRadio(tracks.first())
                    }
                }
            }
            is HomeEvent.PlayHero -> {
                viewModelScope.launch {
                    val matched = _uiState.value.featuredPlaylists.find { it.id == event.banner.targetPlaylistId }
                        ?: repository.getPlaylistById(event.banner.targetPlaylistId.orEmpty())
                        ?: SampleCatalog.samplePlaylists.find { it.id == event.banner.targetPlaylistId }
                    if (matched != null && matched.tracks.isNotEmpty()) {
                        musicController.playTrack(matched.tracks.first(), matched.tracks)
                    } else {
                        val tracks = repository.search(event.banner.targetQuery ?: event.banner.title)
                        val queue = tracks.ifEmpty { SampleCatalog.sampleTracks }
                        if (queue.isNotEmpty()) {
                            musicController.playTrack(queue.first(), queue)
                        }
                    }
                }
            }
        }
    }
}
