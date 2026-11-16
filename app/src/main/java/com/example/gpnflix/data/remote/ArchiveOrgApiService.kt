package com.example.gpnflix.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchiveOrgApiService {
    @GET("advancedsearch.php")
    suspend fun searchMovies(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = DEFAULT_FIELDS,
        @Query("sort[]") sort: String = "downloads desc",
        @Query("rows") rows: Int = 24,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponseDto

    @GET("metadata/{identifier}")
    suspend fun getItemMetadata(
        @Path("identifier") identifier: String
    ): ArchiveItemMetadataResponseDto

    companion object {
        val DEFAULT_FIELDS = listOf(
            "identifier",
            "title",
            "description",
            "creator",
            "year",
            "avg_rating",
            "language",
            "subject",
            "licenseurl",
            "downloads",
            "collection"
        )
    }
}
