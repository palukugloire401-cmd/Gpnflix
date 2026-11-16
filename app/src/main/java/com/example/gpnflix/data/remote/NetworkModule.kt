package com.example.gpnflix.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val ARCHIVE_BASE_URL = "https://archive.org/"

    const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w500"
    const val TMDB_BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"

    @Volatile
    var customTmdbApiKey: String? = null

    @Volatile
    var currentImageQuality: com.example.gpnflix.data.model.ImageQualityMode =
        com.example.gpnflix.data.model.ImageQualityMode.HD

    fun buildTmdbPosterUrl(path: String?): String =
        path?.takeIf { it.isNotBlank() }?.let {
            "https://image.tmdb.org/t/p/${currentImageQuality.tmdbPosterSize}$it"
        }.orEmpty()

    fun buildTmdbBackdropUrl(path: String?): String =
        path?.takeIf { it.isNotBlank() }?.let {
            "https://image.tmdb.org/t/p/${currentImageQuality.tmdbBackdropSize}$it"
        }.orEmpty()

    fun normalizeTmdbKey(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
        if (cleaned.startsWith("Bearer ", ignoreCase = true)) {
            cleaned = cleaned.substring(7).trim()
        }
        return cleaned.replace("\n", "").replace("\r", "").replace(" ", "")
    }

    fun activeTmdbKey(): String {
        val custom = normalizeTmdbKey(customTmdbApiKey)
        if (custom.isNotEmpty()) return custom
        return normalizeTmdbKey(BuildConfig.TMDB_API_KEY)
    }

    fun isV4BearerToken(key: String = activeTmdbKey()): Boolean {
        return key.startsWith("eyJ") && key.count { it == '.' } >= 2
    }

    fun hasValidTmdbKey(): Boolean {
        val key = activeTmdbKey()
        return key.isNotEmpty() &&
            key != "MY_TMDB_API_KEY" &&
            !key.startsWith("MY_")
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val tmdbAuthInterceptor = Interceptor { chain ->
        val original = chain.request()
        val key = activeTmdbKey()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")

        if (key.startsWith("eyJ")) {
            // TMDB API v4 Read Access Token (JWT Bearer)
            requestBuilder.header("Authorization", "Bearer $key")
            chain.proceed(requestBuilder.build())
        } else {
            // TMDB API v3 key query parameter
            val newUrl = original.url.newBuilder()
                .addQueryParameter("api_key", key)
                .build()
            requestBuilder.url(newUrl)
            chain.proceed(requestBuilder.build())
        }
    }

    private val archiveClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "GpnFlix-Android/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val tmdbClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(tmdbAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val tmdbApi: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(tmdbClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApiService::class.java)
    }

    val archiveOrgApi: ArchiveOrgApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ARCHIVE_BASE_URL)
            .client(archiveClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ArchiveOrgApiService::class.java)
    }
}
