package com.spotify.clone.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Replicates Spotify's chunk-caching audio pipeline (CacheMovingWorker / SimpleCache).
 * Caches incoming HTTP audio frames in 128KB chunks inside a 250MB local LRU cache
 * so subsequent plays and seeking consume zero network bandwidth.
 */
@OptIn(UnstableApi::class)
object AudioCacheManager {
    private const val MAX_CACHE_SIZE_BYTES = 250L * 1024L * 1024L // 250 MB
    private var cacheInstance: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cacheInstance == null) {
            val cacheFolder = File(context.cacheDir, "spotify_chunk_cache")
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            cacheInstance = SimpleCache(cacheFolder, evictor, dbProvider)
        }
        return cacheInstance!!
    }

    fun createCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DefaultHttpDataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, upstreamFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getCacheSizeBytes(context: Context): Long {
        return try {
            val cache = getCache(context)
            val space = cache.cacheSpace
            if (space > 0L) space else {
                val cacheFolder = File(context.cacheDir, "spotify_chunk_cache")
                if (cacheFolder.exists()) {
                    cacheFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                } else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun clearCache(context: Context) {
        try {
            val cache = getCache(context)
            val keys = cache.keys.toList()
            for (key in keys) {
                cache.removeResource(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
