package com.spotify.clone.core.data

import android.text.Html
import android.util.Log
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service providing direct access to both Regional (Telugu / Indian) and Global (Western / Worldwide)
 * music catalogs with authentic studio full-length recordings (160kbps / 320kbps) and high-resolution artwork.
 */
class GlobalCatalogService {

    companion object {
        private const val TAG = "GlobalCatalog"
        private val trackQueryCache = java.util.concurrent.ConcurrentHashMap<String, List<Track>>()
        private val playlistQueryCache = java.util.concurrent.ConcurrentHashMap<String, Playlist>()
    }

    private fun openConnection(urlStr: String, referer: String? = null): HttpURLConnection {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 3000
        conn.readTimeout = 4000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        if (referer != null) {
            conn.setRequestProperty("Referer", referer)
        }
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    /**
     * Fetches full-length studio tracks directly from high-fidelity streaming pipeline.
     */
    suspend fun fetchSaavnTracks(query: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = "${query.trim().lowercase()}_$limit"
        val cached = trackQueryCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            return@withContext cached
        }

        val tracks = mutableListOf<Track>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encoded"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONArray("results") ?: return@withContext emptyList()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val rawTitle = item.optString("title", "")
                    if (rawTitle.isBlank()) continue

                    val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val moreInfo = item.optJSONObject("more_info")
                    val fallbackArtist = moreInfo?.optString("music", "Unknown Artist") ?: "Unknown Artist"
                    val rawArtist = item.optString("subtitle", fallbackArtist)
                    val artist = Html.fromHtml(rawArtist, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val rawAlbum = moreInfo?.optString("album", "Single") ?: "Single"
                    val album = Html.fromHtml(rawAlbum, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                    val rawCover = item.optString("image", "")
                    val highResCover = rawCover.replace("150x150", "500x500")

                    val durationSec = moreInfo?.optLong("duration", 210L) ?: 210L
                    val durationMs = durationSec * 1000L

                    val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                    val mediaUri = if (encUrl.isNotBlank()) "saavn_enc:$encUrl" else ""

                    tracks.add(
                        Track(
                            id = "saavn_$id",
                            title = title,
                            artistName = artist,
                            albumName = album,
                            coverArtUrl = highResCover,
                            durationMs = durationMs,
                            mediaUri = mediaUri,
                            isExplicit = item.optString("explicit_content", "0") == "1"
                        )
                    )
                }
                Log.d(TAG, "fetchSaavnTracks for '$query' successfully found ${tracks.size} tracks")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchSaavnTracks for query '$query'", e)
        }
        if (tracks.isNotEmpty()) {
            trackQueryCache[cacheKey] = tracks
        }
        tracks
    }

    private fun fetchAppleTracks(urlStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val conn = openConnection(urlStr)
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONArray("results") ?: return emptyList()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val trackId = item.optLong("trackId", 0L)
                    val title = item.optString("trackName", "")
                    if (title.isBlank()) continue

                    val artist = item.optString("artistName", "Unknown Artist")
                    val album = item.optString("collectionName", "Single")
                    val rawCover = item.optString("artworkUrl100", "")
                    val highResCover = rawCover.replace("100x100bb", "600x600bb")
                    val duration = item.optLong("trackTimeMillis", 210000L)
                    val previewUrl = item.optString("previewUrl", "")
                    val isExplicit = item.optString("trackExplicitness", "") == "explicit"

                    tracks.add(
                        Track(
                            id = "apple_$trackId",
                            title = title,
                            artistName = artist,
                            albumName = album,
                            coverArtUrl = highResCover,
                            durationMs = duration,
                            mediaUri = previewUrl,
                            isExplicit = isExplicit
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching tracks from Apple endpoint: $urlStr", e)
        }
        return tracks
    }

    /**
     * Searches across both Regional (Telugu / Indian) and Global catalogs.
     * Concurrently queries JioSaavn, Apple India, and Apple Global storefronts and merges results.
     */
    suspend fun searchTracks(query: String, limit: Int = 30): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val saavnDeferred = async { fetchSaavnTracks(query, limit = limit) }
        val indiaDeferred = async {
            val urlIndia = "https://itunes.apple.com/search?term=$encoded&country=IN&entity=song&limit=$limit"
            fetchAppleTracks(urlIndia)
        }
        val globalDeferred = async {
            val urlGlobal = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit"
            fetchAppleTracks(urlGlobal)
        }

        val saavnTracks = saavnDeferred.await()
        val inTracks = indiaDeferred.await()
        val globalTracks = globalDeferred.await()

        val combined = mutableListOf<Track>()
        // 1. Regional studio streams first
        combined.addAll(saavnTracks)
        // 2. India storefront
        combined.addAll(inTracks)
        // 3. Global international storefront
        combined.addAll(globalTracks)

        val distinct = combined.distinctBy {
            "${it.title.lowercase().trim()}_${it.artistName.lowercase().trim()}"
        }
        Log.d(TAG, "Search '$query' merged ${distinct.size} tracks across Saavn and Apple storefronts")
        distinct
    }

    /**
     * Fetches dedicated Telugu Top Hits playlist with authentic full-length studio recordings.
     */
    suspend fun getTeluguTopHitsPlaylist(): Playlist = withContext(Dispatchers.IO) {
        val teluguTerms = listOf(
            "chuttamalle devara",
            "pushpa pushpa",
            "sooseki pushpa 2",
            "kurchi madathapetti",
            "bhairava anthem kalki",
            "samajavaragamana",
            "naatu naatu rrr",
            "oo antava pushpa",
            "inkem inkem",
            "fear song devara"
        )

        val deferredTracks = teluguTerms.map { term ->
            async {
                val found = fetchSaavnTracks(term, limit = 1)
                found.firstOrNull()
            }
        }

        val tracks = deferredTracks.awaitAll().filterNotNull().distinctBy { it.id }

        // Fallback if needed
        val finalTracks = if (tracks.isNotEmpty()) {
            tracks
        } else {
            searchTracks("telugu top hits", limit = 10)
        }

        Playlist(
            id = "pl_telugu_top_hits",
            name = "Tollywood Top Hits",
            description = "The biggest Telugu chartbusters from Devara, Pushpa 2, RRR, Kalki 2898 AD & more (Full Studio Versions).",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "ECHO Telugu",
            tracks = finalTracks
        )
    }

    /**
     * Fetches Sid Sriram Telugu Melodies playlist with full studio recordings.
     */
    suspend fun getSidSriramMelodiesPlaylist(): Playlist = withContext(Dispatchers.IO) {
        val tracks = fetchSaavnTracks("sid sriram telugu", limit = 15)
        val finalTracks = if (tracks.isNotEmpty()) tracks else searchTracks("sid sriram telugu", limit = 15)

        Playlist(
            id = "pl_sid_sriram_melodies",
            name = "Sid Sriram Telugu Hits",
            description = "Soulful Telugu melodies sung by Sid Sriram: Inkem Inkem, Samajavaragamana & more (Full Versions).",
            coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
            ownerName = "ECHO Curated",
            tracks = finalTracks
        )
    }

    /**
     * Fetches curated playlist by keyword and storefront.
     */
    suspend fun getCuratedPlaylist(term: String, playlistName: String, coverUrl: String, country: String = "IN"): Playlist = withContext(Dispatchers.IO) {
        val tracks = fetchSaavnTracks(term, limit = 20)
        val finalTracks = if (tracks.isNotEmpty()) tracks else searchTracks(term, limit = 20)

        Playlist(
            id = "global_pl_${term.lowercase().replace(" ", "_")}",
            name = playlistName,
            description = "The hottest tracks right now across the catalog (Full Length).",
            coverArtUrl = coverUrl,
            ownerName = "ECHO Curated",
            tracks = finalTracks
        )
    }

    /**
     * Searches for movie soundtrack albums and playlists matching a film or title query.
     */
    suspend fun searchAlbums(query: String, limit: Int = 6): List<Playlist> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Playlist>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getAlbumResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encoded"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val rawTitle = item.optString("title", "")
                    if (id.isBlank() || rawTitle.isBlank()) continue
                    val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val year = item.optString("year", "")
                    val rawImage = item.optString("image", "")
                    val cover = rawImage.replace("150x150", "500x500")
                    val moreInfo = item.optJSONObject("more_info")
                    val artist = moreInfo?.optString("music", "Original Soundtrack") ?: "Original Soundtrack"

                    albums.add(
                        Playlist(
                            id = "album_saavn_$id",
                            name = title,
                            description = if (year.isNotBlank()) "Movie Soundtrack • $year" else "Movie Soundtrack",
                            coverArtUrl = cover,
                            ownerName = Html.fromHtml(artist, Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                            tracks = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchAlbums for query '$query'", e)
        }
        albums
    }

    /**
     * Fetches complete movie album soundtrack playlist with all full-length studio tracks.
     */
    suspend fun getAlbumPlaylist(albumId: String): Playlist? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&albumid=$albumId"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val rawTitle = json.optString("title", "")
                if (rawTitle.isBlank()) return@withContext null
                val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                val year = json.optString("year", "")
                val rawImage = json.optString("image", "")
                val cover = rawImage.replace("150x150", "500x500")
                val artist = Html.fromHtml(json.optString("primary_artists", "Original Soundtrack"), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                val list = json.optJSONArray("list") ?: return@withContext null

                val tracks = mutableListOf<Track>()
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val songTitle = Html.fromHtml(item.optString("title", ""), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val moreInfo = item.optJSONObject("more_info")
                    val songArtist = Html.fromHtml(item.optString("subtitle", artist), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                    val duration = (moreInfo?.optLong("duration", 210L) ?: 210L) * 1000L
                    val songCover = item.optString("image", cover).replace("150x150", "500x500")

                    tracks.add(
                        Track(
                            id = "saavn_$id",
                            title = songTitle,
                            artistName = songArtist,
                            albumName = title,
                            coverArtUrl = songCover,
                            durationMs = duration,
                            mediaUri = if (encUrl.isNotBlank()) "saavn_enc:$encUrl" else "",
                            isExplicit = item.optString("explicit_content", "0") == "1"
                        )
                    )
                }

                return@withContext Playlist(
                    id = "album_saavn_$albumId",
                    name = title,
                    description = "Official Movie Album • $year • ${tracks.size} songs",
                    coverArtUrl = cover,
                    ownerName = if (artist.isNotBlank()) artist else "Original Soundtrack",
                    tracks = tracks
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch album details for albumId $albumId", e)
        }
        null
    }

    /**
     * Editorial hero banners inspired by Apple Music's "New" showcase.
     */
    fun getEditorialBanners(): List<EditorialBanner> {
        return listOf(
            EditorialBanner(
                id = "hero_devara",
                eyebrow = "",
                title = "Devara: Part 1 Hits",
                subtitle = "Chuttamalle, Fear Song & Daavudi by Anirudh Ravichander",
                coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                targetPlaylistId = "pl_devara",
                targetQuery = "Chuttamalle Devara"
            ),
            EditorialBanner(
                id = "hero_pushpa2",
                eyebrow = "✦ NEW RELEASE",
                title = "Pushpa 2: The Rule",
                subtitle = "Allu Arjun & Devi Sri Prasad chartbusters in studio quality",
                coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
                targetPlaylistId = "pl_pushpa2",
                targetQuery = "Pushpa 2 Pushpa Pushpa"
            ),
            EditorialBanner(
                id = "hero_sita_ramam",
                eyebrow = "✦ TELUGU MELODY MAGIC",
                title = "Sita Ramam: Soulful Romance",
                subtitle = "Inthandham, Oh Sita Hey Rama & Vishal Chandrashekhar's timeless strings",
                coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
                targetPlaylistId = "pl_sita_ramam",
                targetQuery = "Sita Ramam Inthandham"
            ),
            EditorialBanner(
                id = "hero_kalki",
                eyebrow = "✦ SCI-FI SPECTACLE",
                title = "Kalki 2898 AD",
                subtitle = "Bhairava Anthem, Ta Takkara & Santhosh Narayanan's futuristic score",
                coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
                targetPlaylistId = "pl_kalki_2898",
                targetQuery = "Bhairava Anthem Kalki"
            ),
            EditorialBanner(
                id = "hero_ala_vaikunthapurramuloo",
                eyebrow = "✦ ALL-TIME CHARTBUSTER",
                title = "Ala Vaikunthapurramuloo",
                subtitle = "Samajavaragamana, Butta Bomma & Ramuloo Ramulaa by Thaman S",
                coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
                targetPlaylistId = "pl_ala_vaikunthapurramuloo",
                targetQuery = "Samajavaragamana"
            )
        )
    }

    /**
     * Fetches real-time "Best New Songs" directly from Apple Music's top releases RSS feed.
     */
    suspend fun getAppleNewSongs(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/us/music/most-played/$limit/songs.json"
            val conn = openConnection(url)
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONObject("feed")?.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val id = item.optString("id", "")
                        val title = item.optString("name", "")
                        val artist = item.optString("artistName", "")
                        val rawArt = item.optString("artworkUrl100", "")
                        val cover = rawArt.replace("100x100bb", "600x600bb")
                        if (title.isBlank()) continue
                        tracks.add(
                            Track(
                                id = "apple_new_$id",
                                title = title,
                                artistName = artist,
                                albumName = "Apple Music New Releases",
                                coverArtUrl = cover,
                                durationMs = 210000L,
                                mediaUri = "", // dynamically resolved by FullTrackStreamResolver
                                isExplicit = item.optJSONArray("genres")?.toString()?.contains("Explicit") == true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Apple New Songs", e)
        }
        if (tracks.isEmpty()) {
            searchTracks("top hits 2026", limit = limit)
        } else {
            tracks
        }
    }

    /**
     * Fetches real-time new albums from Apple Music's top albums feed.
     */
    suspend fun getAppleNewAlbums(limit: Int = 15): List<Playlist> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Playlist>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/us/music/most-played/$limit/albums.json"
            val conn = openConnection(url)
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONObject("feed")?.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val id = item.optString("id", "")
                        val title = item.optString("name", "")
                        val artist = item.optString("artistName", "")
                        val rawArt = item.optString("artworkUrl100", "")
                        val cover = rawArt.replace("100x100bb", "600x600bb")
                        val releaseDate = item.optString("releaseDate", "2026")
                        val year = releaseDate.take(4)
                        if (title.isBlank()) continue
                        albums.add(
                            Playlist(
                                id = "album_apple_$id",
                                name = title,
                                description = "New Album • $year • $artist",
                                coverArtUrl = cover,
                                ownerName = artist,
                                tracks = emptyList()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Apple New Albums", e)
        }
        albums
    }

    /**
     * Fetches ranked Daily Top 100 charts (merges global chartbusters and top Indian hits).
     */
    suspend fun getDailyTop100(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val usTracks = async { getAppleNewSongs(limit = limit / 2) }
        val inTracks = async { fetchSaavnTracks("top hits 2026", limit = limit / 2) }
        (usTracks.await() + inTracks.await()).distinctBy { it.title.lowercase() }
    }

    /**
     * Fetches official JioSaavn Top Charts (India Superhits Top 50, Trending Today, etc.).
     */
    suspend fun getJioSaavnCharts(): List<Playlist> = withContext(Dispatchers.IO) {
        val charts = mutableListOf<Playlist>()
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=content.getCharts&api_version=4&_format=json&_marker=0&ctx=web6dot0"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val array = org.json.JSONArray(conn.inputStream.bufferedReader().readText())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val rawTitle = obj.optString("title", "")
                    val image = obj.optString("image", "").replace("150x150", "500x500")
                    if (id.isBlank() || rawTitle.isBlank()) continue
                    val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    charts.add(
                        Playlist(
                            id = "saavn_chart_$id",
                            name = title,
                            description = "Official JioSaavn Chart • Top 50 Songs",
                            coverArtUrl = image,
                            ownerName = "JioSaavn Editorial",
                            tracks = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch JioSaavn charts", e)
        }
        charts
    }

    /**
     * Fetches JioSaavn "Trending Now" carousel modules from webapi.getLaunchData.
     */
    suspend fun getJioSaavnTrending(): List<Playlist> = withContext(Dispatchers.IO) {
        val trending = mutableListOf<Playlist>()
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=web6dot0"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val newTrending = json.optJSONArray("new_trending")
                if (newTrending != null) {
                    for (i in 0 until newTrending.length()) {
                        val item = newTrending.optJSONObject(i) ?: continue
                        val id = item.optString("id", "")
                        val rawTitle = item.optString("title", "")
                        val rawSub = item.optString("subtitle", "Trending Now")
                        val image = item.optString("image", "").replace("150x150", "500x500")
                        val type = item.optString("type", "playlist")
                        if (id.isBlank() || rawTitle.isBlank()) continue
                        val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        val sub = Html.fromHtml(rawSub, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        val prefix = if (type == "album") "album_saavn_" else "saavn_pl_"
                        trending.add(
                            Playlist(
                                id = "$prefix$id",
                                name = title,
                                description = if (sub.isNotBlank()) "Trending • $sub" else "Trending on JioSaavn",
                                coverArtUrl = image,
                                ownerName = "JioSaavn Trending",
                                tracks = emptyList()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch JioSaavn trending", e)
        }
        trending
    }

    /**
     * Fetches JioSaavn Artist Radio recommendations.
     */
    suspend fun getJioSaavnArtistRadios(): List<Artist> = withContext(Dispatchers.IO) {
        val radios = mutableListOf<Artist>()
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=web6dot0"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val artistRecos = json.optJSONArray("artist_recos")
                if (artistRecos != null) {
                    for (i in 0 until artistRecos.length()) {
                        val item = artistRecos.optJSONObject(i) ?: continue
                        val id = item.optString("id", "")
                        val rawName = item.optString("title", item.optString("name", ""))
                        val rawArt = item.optString("image", "").replace("150x150", "500x500")
                        if (rawName.isBlank()) continue
                        val name = Html.fromHtml(rawName, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        radios.add(
                            Artist(
                                id = "saavn_radio_$id",
                                name = name,
                                avatarUrl = rawArt,
                                monthlyListeners = 15000000L,
                                isVerified = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch JioSaavn artist radios", e)
        }
        radios
    }

    /**
     * Fetches full 50-song playlist or chart from JioSaavn.
     */
    suspend fun getJioSaavnPlaylist(listId: String): Playlist? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=playlist.getDetails&listid=$listId&api_version=4&_format=json&_marker=0&ctx=web6dot0"
            val conn = openConnection(url, referer = "https://www.jiosaavn.com/")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val rawTitle = json.optString("title", "")
                if (rawTitle.isBlank()) return@withContext null
                val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                val image = json.optString("image", "").replace("150x150", "500x500")
                val subtitle = json.optString("subtitle", "Official Chart")
                val list = json.optJSONArray("list") ?: return@withContext null

                val tracks = mutableListOf<Track>()
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val songTitle = Html.fromHtml(item.optString("title", ""), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val moreInfo = item.optJSONObject("more_info")
                    val songArtist = Html.fromHtml(item.optString("subtitle", ""), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val albumName = Html.fromHtml(moreInfo?.optString("album", title) ?: title, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                    val duration = (moreInfo?.optLong("duration", 210L) ?: 210L) * 1000L
                    val songCover = item.optString("image", image).replace("150x150", "500x500")

                    tracks.add(
                        Track(
                            id = "saavn_$id",
                            title = songTitle,
                            artistName = songArtist,
                            albumName = albumName,
                            coverArtUrl = songCover,
                            durationMs = duration,
                            mediaUri = if (encUrl.isNotBlank()) "saavn_enc:$encUrl" else "",
                            isExplicit = item.optString("explicit_content", "0") == "1"
                        )
                    )
                }

                return@withContext Playlist(
                    id = "saavn_pl_$listId",
                    name = title,
                    description = "$subtitle • ${tracks.size} songs",
                    coverArtUrl = image,
                    ownerName = "JioSaavn Editorial",
                    tracks = tracks
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch JioSaavn playlist details for listId $listId", e)
        }
        null
    }

    /**
     * Fetches regional songs for a selected JioSaavn music language.
     */
    suspend fun getSongsForLanguage(language: String): List<Track> = withContext(Dispatchers.IO) {
        when (language.lowercase()) {
            "telugu" -> fetchSaavnTracks("telugu top hits", limit = 20)
            "hindi" -> fetchSaavnTracks("hindi top hits", limit = 20)
            "punjabi" -> fetchSaavnTracks("punjabi top hits", limit = 20)
            "tamil" -> fetchSaavnTracks("tamil top hits", limit = 20)
            "english" -> searchTracks("top hits 2026", limit = 20)
            else -> fetchSaavnTracks("top hits", limit = 20)
        }
    }
}
