package com.spotify.clone.core.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.spotify.clone.core.database.AppDatabase
import com.spotify.clone.core.database.PlaylistEntity
import com.spotify.clone.core.database.PlaylistTrackCrossRef
import com.spotify.clone.core.database.TrackEntity
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.SongUpgradeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class AudioRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val spotifyApi: SpotifyApiService = SpotifyApiService(),
    private val openMusicApi: OpenMusicApiService = OpenMusicApiService(),
    private val globalCatalog: GlobalCatalogService = GlobalCatalogService()
) {
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()

    suspend fun getHomePlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val inMemory = cachedHomePlaylists
        if (!inMemory.isNullOrEmpty()) {
            return@withContext inMemory
        }

        coroutineScope {
            val topHitsDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) {
                        globalCatalog.getCuratedPlaylist(
                            term = "top hits 2026",
                            playlistName = "Today's Top Hits",
                            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                            country = "US"
                        )
                    }
                }.getOrNull()
            }

            val bollywoodHitsDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) {
                        globalCatalog.getCuratedPlaylist(
                            term = "hindi top hits",
                            playlistName = "Indian Chartbusters",
                            coverUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg"
                        )
                    }
                }.getOrNull()
            }

            val teluguHitsDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) { globalCatalog.getTeluguTopHitsPlaylist() }
                }.getOrNull()
            }

            val chillMixDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) {
                        globalCatalog.getCuratedPlaylist(
                            term = "chill acoustic hits",
                            playlistName = "Midnight Chill",
                            coverUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"
                        )
                    }
                }.getOrNull()
            }

            val melodiesDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) { globalCatalog.getSidSriramMelodiesPlaylist() }
                }.getOrNull()
            }

            val popMixDeferred = async {
                runCatching {
                    withTimeoutOrNull(3000) {
                        globalCatalog.getCuratedPlaylist(
                            term = "the weeknd taylor swift",
                            playlistName = "Global Pop Mix",
                            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                            country = "US"
                        )
                    }
                }.getOrNull()
            }

            val combined = listOfNotNull(
                topHitsDeferred.await(),
                bollywoodHitsDeferred.await(),
                teluguHitsDeferred.await(),
                popMixDeferred.await(),
                chillMixDeferred.await(),
                melodiesDeferred.await()
            ).filter { it.tracks.isNotEmpty() }.toMutableList()

            if (combined.size < 2) {
                combined.addAll(SampleCatalog.samplePlaylists.filter { it.id != "pl_liked_songs" })
            }

            val finalPlaylists = combined.distinctBy { it.id }
            finalPlaylists.forEach { playlistCache[it.id] = it }
            cachedHomePlaylists = finalPlaylists
            finalPlaylists
        }
    }

    fun getUserPlaylists(): Flow<List<Playlist>> {
        return combine(
            playlistDao.getAllPlaylists(),
            playlistDao.getAllPlaylistTracksWithEntities()
        ) { entities, playlistTracks ->
            val tracksByPlaylist = playlistTracks.groupBy(
                keySelector = { it.playlistId },
                valueTransform = { it.track.toDomainModel() }
            )
            entities.map { entity ->
                val tracks = tracksByPlaylist[entity.id].orEmpty()
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    coverArtUrl = entity.coverArtUrl,
                    ownerName = entity.ownerName,
                    tracks = tracks
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getHomeArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val saavnRadios = runCatching {
            withTimeoutOrNull(2500) { globalCatalog.getJioSaavnArtistRadios() }
        }.getOrNull().orEmpty()

        val favorites = runCatching { trackDao.getFavoriteTracks().first().map { it.toDomainModel() } }.getOrDefault(emptyList())
        val recent = runCatching { trackDao.getRecentlyPlayedTracks().first().map { it.toDomainModel() } }.getOrDefault(emptyList())
        val personalizedWorld = com.spotify.clone.core.recommendation.EchoRecommendationEngine
            .getOptimizedHomeArtists(
                recentlyPlayed = recent,
                favorites = favorites,
                limit = 16
            )

        (personalizedWorld + saavnRadios + SampleCatalog.sampleArtists)
            .distinctBy { it.name.lowercase().trim() }
            .take(18)
    }

    fun getFavoriteTracks(): Flow<List<Track>> =
        trackDao.getFavoriteTracks().map { list -> list.map { it.toDomainModel() } }

    fun getRecentlyPlayedTracks(): Flow<List<Track>> =
        trackDao.getRecentlyPlayedTracks().map { list -> list.map { it.toDomainModel() } }

    suspend fun getTrackById(id: String): Track? = withContext(Dispatchers.IO) {
        SampleCatalog.sampleTracksById[id]
            ?: trackDao.getTrackById(id)?.toDomainModel()
    }

    suspend fun recordTrackPlayed(track: Track) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = trackDao.getTrackById(track.id)
            val isFav = existing?.isFavorite ?: track.isFavorite
            val now = System.currentTimeMillis()
            val entity = TrackEntity.fromDomainModel(track.copy(isFavorite = isFav), lastPlayed = now)
            trackDao.insertTrack(entity)
            trackDao.updateLastPlayed(track.id, now)
        }
    }

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = trackDao.getTrackById(track.id)
            val currentFav = existing?.isFavorite ?: track.isFavorite
            val newStatus = !currentFav
            trackDao.insertTrack(
                TrackEntity.fromDomainModel(
                    track.copy(isFavorite = newStatus),
                    lastPlayed = existing?.lastPlayedTimestamp ?: 0L
                )
            )
            trackDao.setFavorite(track.id, newStatus)
            newStatus
        }
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            val globalTop = runCatching { globalCatalog.searchTracks("top hits", limit = 15) }.getOrDefault(emptyList())
            val localTracks = scanLocalAudioFiles()
            return@withContext (globalTop + localTracks).distinctBy { it.id }
        }

        val q = query.trim().lowercase()
        val localMatches = scanLocalAudioFiles().filter {
            it.title.lowercase().contains(q) ||
                it.artistName.lowercase().contains(q) ||
                it.albumName.lowercase().contains(q)
        }

        coroutineScope {
            val globalDef = async { runCatching { globalCatalog.searchTracks(query, limit = 25) }.getOrDefault(emptyList()) }
            val cloudDef = async { runCatching { openMusicApi.searchTracks(query, limit = 15) }.getOrDefault(emptyList()) }
            val spotifyDef = async { runCatching { spotifyApi.searchTracks(query) }.getOrDefault(emptyList()) }

            (globalDef.await() + cloudDef.await() + spotifyDef.await() + localMatches).distinctBy {
                "${it.title.lowercase().trim()}_${it.artistName.lowercase().trim()}"
            }
        }
    }

    suspend fun searchAlbums(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val albums = runCatching { globalCatalog.searchAlbums(query, limit = 8) }.getOrDefault(emptyList())
        albums.forEach { playlistCache[it.id] = it }
        albums
    }

    suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.IO) {
        if (id == "pl_liked_songs") {
            val favorites = trackDao.getFavoriteTracks().first().map { it.toDomainModel() }
            return@withContext Playlist(
                id = "pl_liked_songs",
                name = "Liked Songs",
                description = "Your favorite tracks in one place",
                coverArtUrl = favorites.firstOrNull()?.coverArtUrl ?: "",
                ownerName = "You",
                tracks = favorites
            )
        }

        val userEntity = playlistDao.getPlaylistById(id)
        if (userEntity != null) {
            val tracks = playlistDao.getTracksForPlaylist(id).first().map { it.toDomainModel() }
            return@withContext Playlist(
                id = userEntity.id,
                name = userEntity.name,
                description = userEntity.description,
                coverArtUrl = userEntity.coverArtUrl.ifBlank { tracks.firstOrNull()?.coverArtUrl ?: "" },
                ownerName = userEntity.ownerName,
                tracks = tracks
            )
        }

        val cached = playlistCache[id]
        if (cached != null && cached.tracks.isNotEmpty()) {
            return@withContext cached
        }

        if (id.startsWith("album_saavn_")) {
            globalCatalog.getAlbumPlaylist(id.removePrefix("album_saavn_"))?.let {
                playlistCache[id] = it
                return@withContext it
            }
        }

        if (id.startsWith("saavn_chart_")) {
            globalCatalog.getJioSaavnPlaylist(id.removePrefix("saavn_chart_"))?.let {
                playlistCache[id] = it
                return@withContext it
            }
        }

        if (id.startsWith("saavn_pl_")) {
            globalCatalog.getJioSaavnPlaylist(id.removePrefix("saavn_pl_"))?.let {
                playlistCache[id] = it
                return@withContext it
            }
        }

        if (id.startsWith("audius_pl_")) {
            val tracks = runCatching { openMusicApi.getPlaylistTracks(id) }.getOrDefault(emptyList())
            if (cached != null) {
                val updated = cached.copy(tracks = tracks)
                playlistCache[id] = updated
                return@withContext updated
            } else if (tracks.isNotEmpty()) {
                val fallback = Playlist(
                    id = id,
                    name = "Audius Playlist",
                    description = "${tracks.size} tracks",
                    coverArtUrl = tracks.first().coverArtUrl,
                    ownerName = "Audius",
                    tracks = tracks
                )
                playlistCache[id] = fallback
                return@withContext fallback
            }
        }

        val base = cached
            ?: getHomePlaylists().find { it.id == id }
            ?: SampleCatalog.samplePlaylists.find { it.id == id }

        if (base != null && base.tracks.isEmpty() && base.ownerName.isNotBlank()) {
            val searched = search(base.name).take(15)
            val resolved = base.copy(tracks = searched.ifEmpty { SampleCatalog.sampleTracks })
            playlistCache[id] = resolved
            return@withContext resolved
        }

        base
    }

    suspend fun addTrackToPlaylist(track: Track, playlistId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = trackDao.getTrackById(track.id)
            trackDao.insertTrack(
                TrackEntity.fromDomainModel(
                    track.copy(isFavorite = existing?.isFavorite ?: track.isFavorite),
                    lastPlayed = existing?.lastPlayedTimestamp ?: 0L
                )
            )
            if (playlistDao.countTrackInPlaylist(playlistId, track.id) == 0) {
                val nextPos = playlistDao.getMaxPositionForPlaylist(playlistId) + 1
                playlistDao.addTrackToPlaylist(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        trackId = track.id,
                        position = nextPos
                    )
                )
            }
            val pl = playlistDao.getPlaylistById(playlistId)
            if (pl != null && pl.coverArtUrl.isBlank() && track.coverArtUrl.isNotBlank()) {
                playlistDao.updatePlaylistCover(playlistId, track.coverArtUrl)
            }
        }
    }

    suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String) = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return@withContext
        playlistDao.updatePlaylistDetails(playlistId, cleanName, description.trim())
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist = withContext(Dispatchers.IO) {
        val newId = "pl_custom_${System.currentTimeMillis()}"
        val entity = PlaylistEntity(
            id = newId,
            name = name.trim(),
            description = description.trim(),
            coverArtUrl = ""
        )
        playlistDao.insertPlaylist(entity)
        Playlist(
            id = newId,
            name = entity.name,
            description = entity.description,
            coverArtUrl = "",
            ownerName = "You"
        )
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            playlistDao.clearPlaylistTracks(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }
    }

    private fun hasLocalAudioPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanLocalAudioFiles(forceRefresh: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        if (!hasLocalAudioPermission()) {
            cachedLocalTracks = null
            lastLocalScanMs = 0L
            return@withContext emptyList()
        }
        val cached = cachedLocalTracks
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && (now - lastLocalScanMs) < 60_000L) {
            return@withContext cached
        }
        val localTracks = mutableListOf<Track>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 15000"
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = resolveLocalAlbumArtUri(contentUri, albumId)

                    localTracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artistName = artist,
                            albumName = album,
                            coverArtUrl = artUri,
                            durationMs = duration,
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        cachedLocalTracks = localTracks
        lastLocalScanMs = System.currentTimeMillis()
        localTracks
    }

    private fun resolveLocalAlbumArtUri(contentUri: Uri, albumId: Long): String {
        val legacyUri = if (albumId > 0L) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
        } else {
            ""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && albumId > 0L) {
            val cacheDir = File(context.cacheDir, "local_album_art").apply { if (!exists()) mkdirs() }
            val cachedFile = File(cacheDir, "album_$albumId.jpg")
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                return Uri.fromFile(cachedFile).toString()
            }
            val loaded = runCatching {
                val bmp = context.contentResolver.loadThumbnail(contentUri, Size(320, 320), null)
                try {
                    FileOutputStream(cachedFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    Uri.fromFile(cachedFile).toString()
                } catch (e: Exception) {
                    cachedFile.delete()
                    throw e
                } finally {
                    bmp.recycle()
                }
            }.getOrNull()
            if (!loaded.isNullOrBlank()) return loaded
        }
        return legacyUri
    }

    fun getEditorialBanners(): List<EditorialBanner> = globalCatalog.getEditorialBanners()

    suspend fun getBestNewSongs(): List<Track> = SongUpgradeEngine.upgradeCatalogFast(globalCatalog.getAppleNewSongs(20))

    suspend fun getNewAlbums(): List<Playlist> = globalCatalog.getAppleNewAlbums(15).also { albums ->
        albums.forEach { playlistCache[it.id] = it }
    }

    suspend fun getDailyTop100(): List<Track> = SongUpgradeEngine.upgradeCatalogFast(globalCatalog.getDailyTop100(20))

    /**
     * Fetches a fresh daily pool of new releases across global and regional storefronts,
     * tailored to the user's preferred languages and genres, and upgrades metadata immediately.
     */
    suspend fun getDailyNewSongsFeed(
        preferredLanguages: Set<String> = emptySet(),
        preferredGenres: List<MusicGenre> = emptyList()
    ): List<Track> = withContext(Dispatchers.IO) {
        coroutineScope {
            val appleGlobalDef = async { runCatching { globalCatalog.getAppleNewSongs(20) }.getOrDefault(emptyList()) }
            val saavnLatestInDef = async { runCatching { globalCatalog.fetchSaavnTracks("latest indian songs 2026", limit = 15) }.getOrDefault(emptyList()) }
            val primaryLang = preferredLanguages.firstOrNull { !it.equals("English", ignoreCase = true) && !it.equals("Global", ignoreCase = true) }
                ?: preferredLanguages.firstOrNull()
            val regionalQuery = if (primaryLang != null) {
                WorldOnboardingCatalog.searchQueriesForLanguage(primaryLang).lastOrNull() ?: "new trending songs 2026"
            } else {
                "new trending songs 2026"
            }
            val regionalDef = async {
                if (primaryLang != null && primaryLang !in WorldOnboardingCatalog.INDIAN_LANGUAGES) {
                    runCatching { globalCatalog.searchTracks(regionalQuery, limit = 15) }.getOrDefault(emptyList())
                } else {
                    runCatching { globalCatalog.fetchSaavnTracks(regionalQuery, limit = 15) }.getOrDefault(emptyList())
                }
            }
            val secondaryLang = preferredLanguages.firstOrNull { !it.equals(primaryLang, ignoreCase = true) && !it.equals("Global", ignoreCase = true) }
            val secondaryLangDef = async {
                if (secondaryLang != null) {
                    val q = WorldOnboardingCatalog.searchQueriesForLanguage(secondaryLang).firstOrNull() ?: secondaryLang
                    runCatching { globalCatalog.searchTracks(q, limit = 10) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            val genreQuery = preferredGenres.firstOrNull { it != MusicGenre.ALL }?.searchQueries?.firstOrNull()
            val genreDef = async {
                if (genreQuery != null) runCatching { globalCatalog.fetchSaavnTracks(genreQuery, limit = 12) }.getOrDefault(emptyList())
                else emptyList()
            }

            val merged = appleGlobalDef.await() + regionalDef.await() + secondaryLangDef.await() + saavnLatestInDef.await() + genreDef.await()
            SongUpgradeEngine.upgradeCatalogFast(merged)
        }
    }

    suspend fun getAllPersistedTracks(): List<Track> = withContext(Dispatchers.IO) {
        trackDao.getAllTracks().map { it.toDomainModel() }
    }

    suspend fun persistUpgradedTrack(track: Track) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = trackDao.getTrackById(track.id)
            trackDao.insertTrack(
                TrackEntity.fromDomainModel(
                    track = track.copy(isFavorite = existing?.isFavorite ?: track.isFavorite),
                    lastPlayed = existing?.lastPlayedTimestamp ?: 0L
                )
            )
        }
    }

    suspend fun getJioSaavnCharts(): List<Playlist> = globalCatalog.getJioSaavnCharts()

    suspend fun getJioSaavnTrending(): List<Playlist> = globalCatalog.getJioSaavnTrending()

    companion object {
        private const val MAX_PLAYLIST_CACHE_ENTRIES = 60
        private val playlistCache: MutableMap<String, Playlist> = java.util.Collections.synchronizedMap(
            object : LinkedHashMap<String, Playlist>(32, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Playlist>?): Boolean =
                    size > MAX_PLAYLIST_CACHE_ENTRIES
            }
        )
        @Volatile private var cachedHomePlaylists: List<Playlist>? = null
        @Volatile private var cachedHomeDateKey: String? = null
        @Volatile private var cachedLocalTracks: List<Track>? = null
        @Volatile private var lastLocalScanMs: Long = 0L
        private val _localScanRevision = MutableStateFlow(0L)
        val localScanRevision: StateFlow<Long> = _localScanRevision.asStateFlow()

        fun invalidateDailyCacheIfNewDay(dateKey: String) {
            if (cachedHomeDateKey != dateKey) {
                cachedHomePlaylists = null
                cachedHomeDateKey = dateKey
            }
        }

        fun forceInvalidateHomeCache() {
            cachedHomePlaylists = null
        }

        fun registerGeneratedPlaylists(playlists: List<Playlist>) {
            playlists.forEach { playlistCache[it.id] = it }
        }

        fun notifyAudioPermissionGranted() {
            cachedLocalTracks = null
            lastLocalScanMs = 0L
            _localScanRevision.value = System.currentTimeMillis().coerceAtLeast(_localScanRevision.value + 1L)
        }
    }
}
