package com.example.gpnflix.data.remote

import com.squareup.moshi.Json

data class ArchiveSearchResponseDto(
    @Json(name = "response") val response: ArchiveSearchBodyDto? = null
)

data class ArchiveSearchBodyDto(
    @Json(name = "numFound") val numFound: Int = 0,
    @Json(name = "docs") val docs: List<ArchiveDocDto> = emptyList()
)

data class ArchiveDocDto(
    @Json(name = "identifier") val identifier: String = "",
    @Json(name = "title") val title: Any? = null,
    @Json(name = "description") val description: Any? = null,
    @Json(name = "creator") val creator: Any? = null,
    @Json(name = "year") val year: Any? = null,
    @Json(name = "avg_rating") val avgRating: Any? = null,
    @Json(name = "language") val language: Any? = null,
    @Json(name = "subject") val subject: Any? = null,
    @Json(name = "licenseurl") val licenseUrl: Any? = null,
    @Json(name = "downloads") val downloads: Long? = null,
    @Json(name = "collection") val collection: Any? = null
)

data class ArchiveItemMetadataResponseDto(
    @Json(name = "files") val files: List<ArchiveFileDto> = emptyList(),
    @Json(name = "metadata") val metadata: ArchiveItemMetadataDto? = null
)

data class ArchiveFileDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "format") val format: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "length") val length: String? = null,
    @Json(name = "height") val height: String? = null
)

data class ArchiveItemMetadataDto(
    @Json(name = "identifier") val identifier: String? = null,
    @Json(name = "title") val title: Any? = null,
    @Json(name = "description") val description: Any? = null,
    @Json(name = "creator") val creator: Any? = null,
    @Json(name = "year") val year: Any? = null,
    @Json(name = "language") val language: Any? = null,
    @Json(name = "subject") val subject: Any? = null,
    @Json(name = "licenseurl") val licenseUrl: Any? = null,
    @Json(name = "runtime") val runtime: Any? = null,
    @Json(name = "addeddate") val addedDate: Any? = null
)
