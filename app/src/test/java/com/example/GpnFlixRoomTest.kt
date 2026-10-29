package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.FavoriteEntity
import com.example.gpnflix.data.local.GpnFlixDatabase
import com.example.gpnflix.data.local.WatchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GpnFlixRoomTest {

    private lateinit var db: GpnFlixDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GpnFlixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testWatchHistoryInsertAndRetrieve() = runBlocking {
        val entity = WatchHistoryEntity(
            movieId = "sintel_2010",
            progressMs = 120000L,
            durationMs = 900000L,
            lastWatchedTimestamp = 1000L,
            isCompleted = false
        )
        db.watchHistoryDao().saveProgress(entity)

        val retrieved = db.watchHistoryDao().getProgressForMovie("sintel_2010")
        assertNotNull(retrieved)
        assertEquals(120000L, retrieved?.progressMs)
        assertEquals(900000L, retrieved?.durationMs)
        assertFalse(retrieved?.isCompleted ?: true)

        val continueWatching = db.watchHistoryDao().getContinueWatching().first()
        assertEquals(1, continueWatching.size)
        assertEquals("sintel_2010", continueWatching.first().movieId)
    }

    @Test
    fun testFavoritesInsertAndObserve() = runBlocking {
        val favoriteDao = db.favoriteDao()
        assertFalse(favoriteDao.isFavorite("tears_of_steel").first())

        favoriteDao.addFavorite(FavoriteEntity("tears_of_steel"))
        assertTrue(favoriteDao.isFavorite("tears_of_steel").first())

        val allFavs = favoriteDao.getAllFavorites().first()
        assertEquals(1, allFavs.size)

        favoriteDao.removeFavorite("tears_of_steel")
        assertFalse(favoriteDao.isFavorite("tears_of_steel").first())
    }

    @Test
    fun testDownloadsInsertAndRetrieve() = runBlocking {
        val downloadDao = db.downloadDao()
        val download = DownloadEntity(
            movieId = "bbb_2008",
            title = "Big Buck Bunny",
            posterUrl = "https://example.com/poster.jpg",
            localFilePath = "/data/downloads/bbb.mp4",
            fileSizeBytes = 50000000L,
            downloadedBytes = 50000000L,
            status = "COMPLETED",
            progressPercent = 100
        )
        downloadDao.saveDownload(download)

        val retrieved = downloadDao.getDownload("bbb_2008")
        assertNotNull(retrieved)
        assertEquals("COMPLETED", retrieved?.status)
        assertEquals(100, retrieved?.progressPercent)

        val all = downloadDao.getAllDownloads().first()
        assertEquals(1, all.size)
    }
}
