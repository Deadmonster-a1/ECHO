package com.spotify.clone.core.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.spotify.clone.core.database.AppDatabase
import com.spotify.clone.core.database.PlaylistEntity
import com.spotify.clone.core.database.TrackEntity
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
        // 1. Fetch Tollywood Telugu Top Hits (Devara, Pushpa 2, Kalki, RRR, Guntur Kaaram)
        val teluguTopHitsDeferred = async {
            try { globalCatalog.getTeluguTopHitsPlaylist() } catch (e: Exception) { null }
        }

        // 2. Fetch Sid Sriram Telugu Melodies
        val sidSriramHitsDeferred = async {
            try { globalCatalog.getSidSriramMelodiesPlaylist() } catch (e: Exception) { null }
        }

        // 3. Fetch curated global top hits playlist
        val topHitsDeferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "top hits 2026",
                    playlistName = "Today's Top Hits",
                    coverUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                    country = "IN"
                )
            } catch (e: Exception) { null }
        }

        // 4. Fetch Devara Part 1
        val devaraDeferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "Devara Part 1",
                    playlistName = "Devara: Part 1",
                    coverUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg"
                )
            } catch (e: Exception) { null }
        }

        // 5. Fetch Pushpa 2: The Rule
        val pushpa2Deferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "Pushpa 2 The Rule",
                    playlistName = "Pushpa 2: The Rule",
                    coverUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg"
                )
            } catch (e: Exception) { null }
        }

        // 6. Fetch Sita Ramam
        val sitaRamamDeferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "Sita Ramam",
                    playlistName = "Sita Ramam",
                    coverUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"
                )
            } catch (e: Exception) { null }
        }

        // 7. Fetch Kalki 2898 AD
        val kalkiDeferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "Kalki 2898 AD",
                    playlistName = "Kalki 2898 AD",
                    coverUrl = "https://c.saavncdn.com/264/Kalki-2898-AD-Telugu-2024-20240710180801-500x500.jpg"
                )
            } catch (e: Exception) { null }
        }

        // 8. Fetch Daily Telugu Mix
        val dailyMixDeferred = async {
            try {
                globalCatalog.getCuratedPlaylist(
                    term = "Telugu 2026",
                    playlistName = "Daily Telugu Mix",
                    coverUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg"
                )
            } catch (e: Exception) { null }
        }

        val teluguTopHits = teluguTopHitsDeferred.await()
        val sidSriramHits = sidSriramHitsDeferred.await()
        val topHitsPlaylist = topHitsDeferred.await()
        val devara = devaraDeferred.await()
        val pushpa2 = pushpa2Deferred.await()
        val sitaRamam = sitaRamamDeferred.await()
        val kalki = kalkiDeferred.await()
        val dailyMix = dailyMixDeferred.await()

        val combined = mutableListOf<Playlist>()
        val likedSongs = getPlaylistById("pl_liked_songs")
        if (likedSongs != null) combined.add(likedSongs)
        if (teluguTopHits != null && teluguTopHits.tracks.isNotEmpty()) combined.add(teluguTopHits)
        if (sidSriramHits != null && sidSriramHits.tracks.isNotEmpty()) combined.add(sidSriramHits)
        if (topHitsPlaylist != null && topHitsPlaylist.tracks.isNotEmpty()) combined.add(topHitsPlaylist)
        if (devara != null && devara.tracks.isNotEmpty()) combined.add(devara)
        if (pushpa2 != null && pushpa2.tracks.isNotEmpty()) combined.add(pushpa2)
        if (sitaRamam != null && sitaRamam.tracks.isNotEmpty()) combined.add(sitaRamam)
        if (kalki != null && kalki.tracks.isNotEmpty()) combined.add(kalki)
        if (dailyMix != null && dailyMix.tracks.isNotEmpty()) combined.add(dailyMix)

        val distinct = combined.distinctBy { it.id }.toMutableList()
        if (distinct.size <= 1) {
            distinct.addAll(SampleCatalog.samplePlaylists)
        }
        val finalPlaylists = distinct.distinctBy { it.id }
        finalPlaylists.forEach { playlistCache[it.id] = it }
        finalPlaylists
    }

    fun getUserPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val tracks = playlistDao.getTracksForPlaylist(entity.id).first().map { it.toDomainModel() }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    coverArtUrl = entity.coverArtUrl.ifBlank { "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg" },
                    ownerName = entity.ownerName,
                    tracks = tracks
                )
            }
        }
    }

    suspend fun getHomeArtists(): List<Artist> = withContext(Dispatchers.IO) {
        SampleCatalog.sampleArtists
    }

    fun getFavoriteTracks(): Flow<List<Track>> {
        return trackDao.getFavoriteTracks().map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        val newStatus = !track.isFavorite
        trackDao.insertTrack(TrackEntity.fromDomainModel(track.copy(isFavorite = newStatus)))
        trackDao.setFavorite(track.id, newStatus)
        newStatus
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            val globalTop = try {
                globalCatalog.searchTracks("top hits", limit = 15)
            } catch (e: Exception) {
                emptyList()
            }
            val localTracks = scanLocalAudioFiles()
            val allTracks = if (globalTop.isNotEmpty()) globalTop + localTracks else localTracks
            return@withContext allTracks.distinctBy { it.id }
        }

        val q = query.trim().lowercase()
        val localMatches = scanLocalAudioFiles().filter {
            it.title.lowercase().contains(q) ||
            it.artistName.lowercase().contains(q) ||
            it.albumName.lowercase().contains(q)
        }

        // 1. Live Global 100M+ catalog search (Coldplay, Taylor Swift, Drake, Eminem, etc.)
        val globalResults = try {
            globalCatalog.searchTracks(query, limit = 25)
        } catch (e: Exception) {
            emptyList()
        }

        // 2. Live Cloud search across open decentralized network
        val cloudResults = try {
            openMusicApi.searchTracks(query, limit = 15)
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Live Spotify Catalog search (if Premium developer credentials active)
        val spotifyResults = try {
            spotifyApi.searchTracks(query)
        } catch (e: Exception) {
            emptyList()
        }

        (globalResults + cloudResults + spotifyResults + localMatches).distinctBy {
            "${it.title.lowercase().trim()}_${it.artistName.lowercase().trim()}"
        }
    }

    suspend fun searchAlbums(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val albums = globalCatalog.searchAlbums(query, limit = 6)
        albums.forEach { playlistCache[it.id] = it }
        albums
    }

    suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.IO) {
        if (id == "pl_liked_songs") {
            val favorites = trackDao.getFavoriteTracks().first().map { it.toDomainModel() }
            return@withContext Playlist(
                id = "pl_liked_songs",
                name = "Liked Songs",
                description = "Your favorite tracks and personal collection in one place.",
                coverArtUrl = "",
                ownerName = "You",
                tracks = favorites
            )
        }

        val cached = playlistCache[id]
        if (cached != null && cached.tracks.isNotEmpty()) {
            return@withContext cached
        }

        if (id.startsWith("album_saavn_")) {
            val albumId = id.removePrefix("album_saavn_")
            val albumPlaylist = globalCatalog.getAlbumPlaylist(albumId)
            if (albumPlaylist != null) {
                playlistCache[id] = albumPlaylist
                return@withContext albumPlaylist
            }
        }

        if (id.startsWith("saavn_chart_")) {
            val listId = id.removePrefix("saavn_chart_")
            val chartPlaylist = globalCatalog.getJioSaavnPlaylist(listId)
            if (chartPlaylist != null) {
                playlistCache[id] = chartPlaylist
                return@withContext chartPlaylist
            }
        }

        if (id.startsWith("saavn_pl_")) {
            val listId = id.removePrefix("saavn_pl_")
            val jioPlaylist = globalCatalog.getJioSaavnPlaylist(listId)
            if (jioPlaylist != null) {
                playlistCache[id] = jioPlaylist
                return@withContext jioPlaylist
            }
        }

        val basePlaylist = cached
            ?: getHomePlaylists().find { it.id == id }
            ?: SampleCatalog.samplePlaylists.find { it.id == id }
            ?: Playlist(
                id = id,
                name = "ECHO Curated Selection",
                description = "Hand-picked listening experience in Studio Fidelity",
                coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
                ownerName = "ECHO Editorial",
                tracks = SampleCatalog.sampleTracks
            )

        if (basePlaylist.tracks.isEmpty()) {
            if (id.startsWith("audius_pl_")) {
                val tracks = try { openMusicApi.getPlaylistTracks(id) } catch (e: Exception) { emptyList() }
                if (tracks.isNotEmpty()) {
                    val updated = basePlaylist.copy(tracks = tracks)
                    playlistCache[id] = updated
                    return@withContext updated
                }
            }
            val fallbackTracks = SampleCatalog.sampleTracks
            val updated = basePlaylist.copy(tracks = fallbackTracks)
            playlistCache[id] = updated
            return@withContext updated
        }
        playlistCache[id] = basePlaylist
        basePlaylist
    }

    suspend fun isFavorite(trackId: String): Boolean = withContext(Dispatchers.IO) {
        trackDao.getTrackById(trackId)?.isFavorite ?: false
    }

    suspend fun addTrackToPlaylist(track: Track, playlistId: String) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromDomainModel(track))
        val crossRef = com.spotify.clone.core.database.PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = track.id,
            position = System.currentTimeMillis().toInt()
        )
        playlistDao.addTrackToPlaylist(crossRef)
    }

    companion object {
        private val playlistCache = java.util.concurrent.ConcurrentHashMap<String, Playlist>()
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist = withContext(Dispatchers.IO) {
        val newId = "pl_custom_${System.currentTimeMillis()}"
        val entity = PlaylistEntity(
            id = newId,
            name = name,
            description = description,
            coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg"
        )
        playlistDao.insertPlaylist(entity)
        Playlist(
            id = newId,
            name = name,
            description = description,
            coverArtUrl = entity.coverArtUrl,
            ownerName = "You"
        )
    }

    suspend fun scanLocalAudioFiles(): List<Track> = withContext(Dispatchers.IO) {
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
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Track"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val albumId = it.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    localTracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artistName = artist,
                            albumName = album,
                            coverArtUrl = artUri.toString(),
                            durationMs = duration,
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localTracks
    }

    fun getEditorialBanners(): List<EditorialBanner> {
        return globalCatalog.getEditorialBanners()
    }

    suspend fun getBestNewSongs(): List<Track> {
        return globalCatalog.getAppleNewSongs(20)
    }

    suspend fun getNewAlbums(): List<Playlist> {
        return globalCatalog.getAppleNewAlbums(15)
    }

    suspend fun getDailyTop100(): List<Track> {
        return globalCatalog.getDailyTop100(20)
    }

    suspend fun getJioSaavnCharts(): List<Playlist> {
        return globalCatalog.getJioSaavnCharts()
    }

    suspend fun getJioSaavnTrending(): List<Playlist> {
        return globalCatalog.getJioSaavnTrending()
    }

    suspend fun getJioSaavnArtistRadios(): List<Artist> {
        return globalCatalog.getJioSaavnArtistRadios()
    }

    suspend fun getSongsForLanguage(lang: String): List<Track> {
        return globalCatalog.getSongsForLanguage(lang)
    }
}
