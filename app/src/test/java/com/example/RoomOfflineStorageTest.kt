package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gpnflix.data.local.DownloadDao
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.DownloadStatus
import com.example.gpnflix.data.local.GpnFlixDatabase
import com.example.gpnflix.data.local.VideoMetadataDao
import com.example.gpnflix.data.local.toVideoMetadataEntity
import com.example.gpnflix.data.model.Movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomOfflineStorageTest {

    private lateinit var database: GpnFlixDatabase
    private lateinit var videoMetadataDao: VideoMetadataDao
    private lateinit var downloadDao: DownloadDao

    private val sampleMovie = Movie(
        id = "bbb_test",
        title = "Big Buck Bunny",
        originalTitle = "Big Buck Bunny",
        synopsis = "Un lapin face à trois rongeurs.",
        durationMinutes = 10,
        releaseYear = 2008,
        rating = 4.8f,
        posterUrl = "https://example.com/poster.jpg",
        backdropUrl = "https://example.com/backdrop.jpg",
        videoUrl = "https://example.com/bbb.mp4",
        category = "Animation",
        genres = listOf("Animation", "Comédie"),
        director = "Sacha Goedegebure",
        cast = listOf("Jan Morgenstern"),
        language = "Français",
        licenseType = "Creative Commons BY 3.0",
        isDownloadable = true,
        isFeatured = true
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GpnFlixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoMetadataDao = database.videoMetadataDao()
        downloadDao = database.downloadDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `video metadata entity is persisted and searched via Room`() = runTest {
        videoMetadataDao.upsertVideo(sampleMovie.toVideoMetadataEntity())

        val stored = videoMetadataDao.getVideoById("bbb_test")
        assertNotNull(stored)
        assertEquals("Big Buck Bunny", stored?.title)
        assertEquals(2008, stored?.releaseYear)

        val searchResults = videoMetadataDao.searchVideos(
            query = "Bunny",
            category = "Animation",
            year = 2008,
            language = "Français"
        ).first()

        assertEquals(1, searchResults.size)
        assertEquals(listOf("Animation", "Comédie"), searchResults.first().toMovie().genres)
    }

    @Test
    fun `download lifecycle and metadata relation are tracked via Room`() = runTest {
        videoMetadataDao.upsertVideo(sampleMovie.toVideoMetadataEntity())

        downloadDao.saveDownload(
            DownloadEntity(
                movieId = sampleMovie.id,
                title = sampleMovie.title,
                posterUrl = sampleMovie.posterUrl,
                videoUrl = sampleMovie.videoUrl,
                localFilePath = "/data/user/0/com.example/files/downloads/bbb_test.mp4",
                quality = "1080p HD",
                fileSizeBytes = 10_000_000L,
                downloadedBytes = 0L,
                status = DownloadStatus.QUEUED,
                progressPercent = 0
            )
        )

        downloadDao.updateProgress(
            movieId = sampleMovie.id,
            downloadedBytes = 5_000_000L,
            fileSizeBytes = 10_000_000L,
            progressPercent = 50,
            status = DownloadStatus.DOWNLOADING
        )

        val midState = downloadDao.getDownload(sampleMovie.id)
        assertEquals(DownloadStatus.DOWNLOADING, midState?.status)
        assertEquals(50, midState?.progressPercent)

        downloadDao.updateStatus(sampleMovie.id, DownloadStatus.PAUSED)
        assertEquals(DownloadStatus.PAUSED, downloadDao.getDownload(sampleMovie.id)?.status)

        downloadDao.updateProgress(
            movieId = sampleMovie.id,
            downloadedBytes = 10_000_000L,
            fileSizeBytes = 10_000_000L,
            progressPercent = 100,
            status = DownloadStatus.COMPLETED
        )

        val joined = downloadDao.observeDownloadsWithMetadata().first()
        assertEquals(1, joined.size)
        assertEquals(DownloadStatus.COMPLETED, joined.first().download.status)
        assertEquals("Sacha Goedegebure", joined.first().metadata?.director)
        assertEquals(10_000_000L, downloadDao.observeTotalDownloadedBytes().first())

        downloadDao.deleteDownload(sampleMovie.id)
        assertNull(downloadDao.getDownload(sampleMovie.id))
    }
}
