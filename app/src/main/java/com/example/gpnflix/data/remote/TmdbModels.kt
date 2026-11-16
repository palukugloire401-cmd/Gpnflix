package com.example.gpnflix.data.remote

import com.squareup.moshi.Json

data class TmdbPagedResponse<T>(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<T> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 1,
    @Json(name = "total_results") val totalResults: Int = 0
)

data class TmdbMovieDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "popularity") val popularity: Double? = null
)

data class TmdbTvDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "popularity") val popularity: Double? = null
)

data class TmdbGenreListResponse(
    @Json(name = "genres") val genres: List<TmdbGenreDto> = emptyList()
)

data class TmdbGenreDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

data class TmdbMovieDetailsDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "genres") val genres: List<TmdbGenreDto> = emptyList(),
    @Json(name = "production_companies") val productionCompanies: List<TmdbCompanyDto> = emptyList(),
    @Json(name = "videos") val videos: TmdbVideoListDto? = null,
    @Json(name = "credits") val credits: TmdbCreditsDto? = null,
    @Json(name = "images") val images: TmdbImagesResponseDto? = null
)

data class TmdbTvDetailsDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "original_language") val originalLanguage: String? = null,
    @Json(name = "genres") val genres: List<TmdbGenreDto> = emptyList(),
    @Json(name = "created_by") val createdBy: List<TmdbCreatorDto> = emptyList(),
    @Json(name = "seasons") val seasons: List<TmdbSeasonSummaryDto> = emptyList(),
    @Json(name = "videos") val videos: TmdbVideoListDto? = null,
    @Json(name = "credits") val credits: TmdbCreditsDto? = null,
    @Json(name = "images") val images: TmdbImagesResponseDto? = null
)

data class TmdbCreatorDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = ""
)

data class TmdbSeasonSummaryDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "name") val name: String = "",
    @Json(name = "episode_count") val episodeCount: Int = 0,
    @Json(name = "poster_path") val posterPath: String? = null
)

data class TmdbSeasonDetailsDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "name") val name: String = "",
    @Json(name = "episodes") val episodes: List<TmdbEpisodeDto> = emptyList()
)

data class TmdbEpisodeDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "episode_number") val episodeNumber: Int = 1,
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "name") val name: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "air_date") val airDate: String? = null
)

data class TmdbImagesResponseDto(
    @Json(name = "backdrops") val backdrops: List<TmdbImageDto> = emptyList(),
    @Json(name = "posters") val posters: List<TmdbImageDto> = emptyList(),
    @Json(name = "logos") val logos: List<TmdbImageDto> = emptyList()
)

data class TmdbImageDto(
    @Json(name = "file_path") val filePath: String = "",
    @Json(name = "aspect_ratio") val aspectRatio: Double? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

data class TmdbCompanyDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = ""
)

data class TmdbVideoListDto(
    @Json(name = "results") val results: List<TmdbVideoDto> = emptyList()
)

data class TmdbVideoDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "key") val key: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "site") val site: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "official") val official: Boolean = false,
    @Json(name = "iso_639_1") val iso6391: String? = null
)

data class TmdbCreditsDto(
    @Json(name = "cast") val cast: List<TmdbCastDto> = emptyList(),
    @Json(name = "crew") val crew: List<TmdbCrewDto> = emptyList()
)

data class TmdbCastDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "character") val character: String? = null
)

data class TmdbCrewDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "job") val job: String? = null
)
