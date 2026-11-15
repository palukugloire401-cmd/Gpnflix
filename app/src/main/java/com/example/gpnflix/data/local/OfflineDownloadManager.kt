package com.example.gpnflix.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.gpnflix.data.model.Movie
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages real background HTTP video downloads into app-scoped storage and persists
 * download progress & lifecycle state in Room via [DownloadDao].
 */
class OfflineDownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, Boolean>()
    private val deleteFlags = ConcurrentHashMap<String, Boolean>()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val downloadsDir: File
        get() = File(context.filesDir, "downloads").apply { mkdirs() }

    fun getFinalFile(movieId: String): File = File(downloadsDir, "$movieId.mp4")
    private fun getPartFile(movieId: String): File = File(downloadsDir, "$movieId.mp4.part")

    suspend fun reconcileOnStartup() = withContext(Dispatchers.IO) {
        val interrupted = downloadDao.getInterruptedDownloads()
        for (entity in interrupted) {
            val partFile = getPartFile(entity.movieId)
            val finalFile = getFinalFile(entity.movieId)
            when {
                finalFile.exists() && finalFile.length() > 0L -> {
                    downloadDao.updateProgress(
                        movieId = entity.movieId,
                        downloadedBytes = finalFile.length(),
                        fileSizeBytes = finalFile.length(),
                        progressPercent = 100,
                        status = DownloadStatus.COMPLETED
                    )
                }
                partFile.exists() && partFile.length() > 0L -> {
                    downloadDao.updateStatus(
                        movieId = entity.movieId,
                        status = DownloadStatus.PAUSED,
                        errorMessage = null
                    )
                }
                else -> {
                    downloadDao.updateStatus(
                        movieId = entity.movieId,
                        status = DownloadStatus.FAILED,
                        errorMessage = "Téléchargement interrompu"
                    )
                }
            }
        }
    }

    fun startOrResumeDownload(movie: Movie, quality: String, wifiOnly: Boolean) {
        if (!movie.isDownloadable) return
        if (activeJobs[movie.id]?.isActive == true) return

        pauseFlags.remove(movie.id)
        deleteFlags.remove(movie.id)

        val job = scope.launch {
            val finalFile = getFinalFile(movie.id)
            val partFile = getPartFile(movie.id)
            val existingEntity = downloadDao.getDownload(movie.id)

            // If file is already complete on disk, ensure Room reflects COMPLETED
            if (existingEntity?.status == DownloadStatus.COMPLETED &&
                finalFile.exists() &&
                finalFile.length() > 0L
            ) {
                return@launch
            }

            val existingPartBytes = if (partFile.exists()) partFile.length() else 0L
            val initialTotal = existingEntity?.fileSizeBytes ?: 0L
            val initialPercent = if (initialTotal > 0L) {
                ((existingPartBytes * 100L) / initialTotal).toInt().coerceIn(0, 99)
            } else {
                0
            }

            downloadDao.saveDownload(
                DownloadEntity(
                    movieId = movie.id,
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    videoUrl = movie.videoUrl,
                    localFilePath = finalFile.absolutePath,
                    quality = quality,
                    fileSizeBytes = initialTotal,
                    downloadedBytes = existingPartBytes,
                    status = DownloadStatus.QUEUED,
                    progressPercent = initialPercent,
                    errorMessage = null,
                    createdTimestamp = existingEntity?.createdTimestamp ?: System.currentTimeMillis(),
                    downloadedTimestamp = System.currentTimeMillis()
                )
            )

            if (!isNetworkSuitable(wifiOnly)) {
                val msg = if (wifiOnly) {
                    "Connexion Wi-Fi requise selon vos paramètres."
                } else {
                    "Aucune connexion Internet disponible."
                }
                downloadDao.updateStatus(movie.id, DownloadStatus.FAILED, msg)
                return@launch
            }

            try {
                executeStreamingDownload(movie, partFile, finalFile)
            } catch (ce: CancellationException) {
                when {
                    deleteFlags.remove(movie.id) == true -> {
                        partFile.delete()
                        finalFile.delete()
                        downloadDao.deleteDownload(movie.id)
                    }
                    pauseFlags.remove(movie.id) == true -> {
                        val savedBytes = if (partFile.exists()) partFile.length() else 0L
                        val currentTotal = downloadDao.getDownload(movie.id)?.fileSizeBytes ?: 0L
                        val pct = if (currentTotal > 0L) {
                            ((savedBytes * 100L) / currentTotal).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        downloadDao.updateProgress(
                            movieId = movie.id,
                            downloadedBytes = savedBytes,
                            fileSizeBytes = currentTotal,
                            progressPercent = pct,
                            status = DownloadStatus.PAUSED
                        )
                    }
                }
                throw ce
            } catch (e: Exception) {
                val reason = e.localizedMessage?.take(120) ?: "Erreur réseau pendant le téléchargement"
                downloadDao.updateStatus(
                    movieId = movie.id,
                    status = DownloadStatus.FAILED,
                    errorMessage = reason
                )
            } finally {
                activeJobs.remove(movie.id)
            }
        }

        activeJobs[movie.id] = job
    }

    private suspend fun executeStreamingDownload(
        movie: Movie,
        partFile: File,
        finalFile: File
    ) {
        val resumeOffset = if (partFile.exists()) partFile.length() else 0L
        val requestBuilder = Request.Builder().url(movie.videoUrl)
        if (resumeOffset > 0L) {
            requestBuilder.addHeader("Range", "bytes=$resumeOffset-")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Erreur serveur HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Flux vidéo vide")
            val isPartial = response.code == 206
            val appendMode = isPartial && resumeOffset > 0L
            val contentLength = body.contentLength()
            val totalBytes = when {
                appendMode && contentLength > 0L -> resumeOffset + contentLength
                contentLength > 0L -> contentLength
                else -> (movie.durationMinutes * 10L * 1024L * 1024L).coerceAtLeast(1L)
            }

            var downloaded = if (appendMode) resumeOffset else 0L
            val initialPct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 99)
            downloadDao.updateProgress(
                movieId = movie.id,
                downloadedBytes = downloaded,
                fileSizeBytes = totalBytes,
                progressPercent = initialPct,
                status = DownloadStatus.DOWNLOADING
            )

            body.byteStream().use { input ->
                FileOutputStream(partFile, appendMode).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var lastReportTime = System.currentTimeMillis()
                    var lastReportedPct = initialPct

                    while (true) {
                        if (pauseFlags[movie.id] == true || deleteFlags[movie.id] == true) {
                            throw CancellationException("Download interrupted by user")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 99)
                        if (pct - lastReportedPct >= 2 || now - lastReportTime >= 250L) {
                            lastReportTime = now
                            lastReportedPct = pct
                            downloadDao.updateProgress(
                                movieId = movie.id,
                                downloadedBytes = downloaded,
                                fileSizeBytes = totalBytes,
                                progressPercent = pct,
                                status = DownloadStatus.DOWNLOADING
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            val finalSize = finalFile.length().coerceAtLeast(downloaded)
            downloadDao.updateProgress(
                movieId = movie.id,
                downloadedBytes = finalSize,
                fileSizeBytes = finalSize,
                progressPercent = 100,
                status = DownloadStatus.COMPLETED
            )
        }
    }

    suspend fun pauseDownload(movieId: String) {
        pauseFlags[movieId] = true
        val job = activeJobs[movieId]
        if (job != null && job.isActive) {
            job.cancel()
        } else {
            val partFile = getPartFile(movieId)
            val savedBytes = if (partFile.exists()) partFile.length() else 0L
            val total = downloadDao.getDownload(movieId)?.fileSizeBytes ?: 0L
            val pct = if (total > 0L) ((savedBytes * 100L) / total).toInt().coerceIn(0, 99) else 0
            downloadDao.updateProgress(
                movieId = movieId,
                downloadedBytes = savedBytes,
                fileSizeBytes = total,
                progressPercent = pct,
                status = DownloadStatus.PAUSED
            )
        }
    }

    suspend fun deleteDownload(movieId: String) = withContext(Dispatchers.IO) {
        deleteFlags[movieId] = true
        val job = activeJobs[movieId]
        if (job != null && job.isActive) {
            job.cancel()
        }
        getPartFile(movieId).delete()
        getFinalFile(movieId).delete()
        downloadDao.deleteDownload(movieId)
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        downloadsDir.listFiles()?.forEach { it.delete() }
        downloadDao.clearAllDownloads()
    }

    private fun isNetworkSuitable(wifiOnly: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false
        if (!wifiOnly) return true
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
