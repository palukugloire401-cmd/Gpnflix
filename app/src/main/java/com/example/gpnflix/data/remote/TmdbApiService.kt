package com.example.gpnflix.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("language") language: String,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbMovieDto>

    @GET("trending/tv/week")
    suspend fun getTrendingTv(
        @Query("language") language: String,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbTvDto>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("language") language: String,
        @Query("region") region: String,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbMovieDto>

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("language") language: String,
        @Query("region") region: String,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbMovieDto>

    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("language") language: String
    ): TmdbGenreListResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("language") language: String
    ): TmdbGenreListResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("language") language: String,
        @Query("region") region: String,
        @Query("with_genres") genreIds: String? = null,
        @Query("with_keywords") keywords: String? = null,
        @Query("primary_release_year") year: Int? = null,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbMovieDto>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("language") language: String,
        @Query("with_genres") genreIds: String? = null,
        @Query("first_air_date_year") year: Int? = null,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbTvDto>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("region") region: String,
        @Query("year") year: Int? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbMovieDto>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("first_air_date_year") year: Int? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbPagedResponse<TmdbTvDto>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos,images",
        @Query("include_video_language") includeVideoLanguage: String = "fr,en,null",
        @Query("include_image_language") includeImageLanguage: String = "fr,null,en"
    ): TmdbMovieDetailsDto

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos,images",
        @Query("include_video_language") includeVideoLanguage: String = "fr,en,null",
        @Query("include_image_language") includeImageLanguage: String = "fr,null,en"
    ): TmdbTvDetailsDto

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String
    ): TmdbSeasonDetailsDto

    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(
        @Path("movie_id") movieId: Int,
        @Query("include_image_language") includeImageLanguage: String = "fr,null,en"
    ): TmdbImagesResponseDto

    @GET("tv/{tv_id}/images")
    suspend fun getTvImages(
        @Path("tv_id") tvId: Int,
        @Query("include_image_language") includeImageLanguage: String = "fr,null,en"
    ): TmdbImagesResponseDto
}
