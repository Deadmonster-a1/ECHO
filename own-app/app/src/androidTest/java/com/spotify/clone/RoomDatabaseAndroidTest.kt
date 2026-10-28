package com.spotify.clone

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spotify.clone.core.database.AppDatabase
import com.spotify.clone.core.database.PlaylistDao
import com.spotify.clone.core.database.PlaylistEntity
import com.spotify.clone.core.database.PlaylistTrackCrossRef
import com.spotify.clone.core.database.TrackDao
import com.spotify.clone.core.database.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseAndroidTest {

    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trackDao = database.trackDao()
        playlistDao = database.playlistDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTrack() = runBlocking {
        val track = TrackEntity(
            id = "test_track_1",
            title = "Test Song",
            artistName = "ECHO Artist",
            albumName = "ECHO Album",
            durationMs = 210000L,
            coverArtUrl = "https://example.com/art.jpg",
            mediaUri = "https://example.com/audio.mp3",
            isFavorite = false
        )

        trackDao.insertTrack(track)
        val loaded = trackDao.getTrackById("test_track_1")

        assertNotNull("Track should be retrieved from database", loaded)
        assertEquals("Test Song", loaded?.title)
        assertEquals("ECHO Artist", loaded?.artistName)
    }

    @Test
    fun favoriteTrackToggling() = runBlocking {
        val track = TrackEntity(
            id = "fav_track_1",
            title = "Favorite Song",
            artistName = "Echo Star",
            albumName = "Echo Master",
            durationMs = 180000L,
            coverArtUrl = "",
            mediaUri = "https://example.com/audio.mp3",
            isFavorite = false
        )

        trackDao.insertTrack(track)
        trackDao.setFavorite("fav_track_1", true)

        val favorites = trackDao.getFavoriteTracks().first()
        assertEquals(1, favorites.size)
        assertEquals("fav_track_1", favorites[0].id)
        assertTrue(favorites[0].isFavorite)
    }

    @Test
    fun playlistAndTrackAssociation() = runBlocking {
        val playlist = PlaylistEntity(
            id = "pl_1",
            name = "ECHO Favorites",
            description = "Top tracks",
            coverArtUrl = ""
        )
        val track = TrackEntity(
            id = "track_assoc_1",
            title = "Associated Song",
            artistName = "Artist",
            albumName = "Album",
            durationMs = 150000L,
            coverArtUrl = "",
            mediaUri = "https://example.com/audio.mp3"
        )
        val crossRef = PlaylistTrackCrossRef(
            playlistId = "pl_1",
            trackId = "track_assoc_1",
            position = 0
        )

        playlistDao.insertPlaylist(playlist)
        trackDao.insertTrack(track)
        playlistDao.addTrackToPlaylist(crossRef)

        val playlistTracks = playlistDao.getTracksForPlaylist("pl_1").first()
        assertEquals(1, playlistTracks.size)
        assertEquals("Associated Song", playlistTracks[0].title)
    }

    @Test
    fun migration1to2Execution() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val helper = Room.databaseBuilder(context, AppDatabase::class.java, "test_migration.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        val openHelper = helper.openHelper
        val db: SupportSQLiteDatabase = openHelper.writableDatabase

        // Execute migration indices manually to verify syntax against real SQLite engine
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify index exists by querying sqlite_master
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_tracks_isFavorite'")
        assertTrue("index_tracks_isFavorite must exist in SQLite", cursor.moveToFirst())
        cursor.close()
        helper.close()
    }
}
