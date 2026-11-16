package com.example.gpnflix.data.model

data class CategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val bannerUrl: String = "",
    val archiveQuery: String = "",
    val tmdbGenreId: Int? = null
)
