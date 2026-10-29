package com.example.gpnflix.data.repository

import android.content.Context
import com.example.gpnflix.data.local.DownloadDao
import com.example.gpnflix.data.local.DownloadEntity
import com.example.gpnflix.data.local.FavoriteDao
import com.example.gpnflix.data.local.FavoriteEntity
import com.example.gpnflix.data.local.GpnFlixDatabase
import com.example.gpnflix.data.local.WatchHistoryDao
import com.example.gpnflix.data.local.WatchHistoryEntity
import com.example.gpnflix.data.model.CategoryItem
import com.example.gpnflix.data.model.Movie
import com.example.gpnflix.data.model.UpcomingMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File

class MovieRepository(
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val downloadDao: DownloadDao,
    private val context: Context
) {

    // 100% Legal, Open Source, Creative Commons, and Public Domain Catalogue
    private val catalogue: List<Movie> = listOf(
        Movie(
            id = "bbb_2008",
            title = "Big Buck Bunny",
            originalTitle = "Big Buck Bunny (Peach Open Movie Project)",
            synopsis = "Un lapin géant et bienveillant voit son havre de paix forestier perturbé par trois rongeurs espiègles qui maltraitent les créatures innocentes. Il décide alors d'élaborer un plan d'une ingéniosité redoutable pour leur donner une leçon inoubliable.",
            durationMinutes = 10,
            releaseYear = 2008,
            rating = 4.8f,
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            category = "Animation",
            genres = listOf("Animation", "Comédie", "Famille"),
            director = "Sacha Goedegebure",
            cast = listOf("Jan Morgenstern", "Ton Roosendaal", "Campbell Barton"),
            language = "Français",
            licenseType = "Creative Commons BY 3.0 (Blender Foundation)",
            isDownloadable = true,
            isFeatured = true,
            isTrending = true,
            isAwardWinner = true
        ),
        Movie(
            id = "tears_of_steel",
            title = "Tears of Steel",
            originalTitle = "Tears of Steel (Mango Open Movie Project)",
            synopsis = "Dans un Amsterdam futuriste et dystopique, un groupe de survivants et scientifiques tente de sauver le monde de l'invasion de robots destructeurs en modifiant la mémoire émotionnelle d'une femme cyborg.",
            durationMinutes = 12,
            releaseYear = 2012,
            rating = 4.7f,
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            category = "Science-Fiction",
            genres = listOf("Science-Fiction", "Action", "Cyberpunk"),
            director = "Ian Hubert",
            cast = listOf("Derek de Lint", "Sergio Hasselbaink", "Rogier Schippers", "Denise Rebergen"),
            language = "Anglais",
            licenseType = "Creative Commons BY 3.0 (Blender Foundation)",
            isDownloadable = true,
            isFeatured = true,
            isTrending = true,
            isAwardWinner = true
        ),
        Movie(
            id = "sintel_2010",
            title = "Sintel",
            originalTitle = "Sintel (Durian Open Movie Project)",
            synopsis = "Une jeune guerrière solitaire recueille et soigne un bébé dragon blessé qu'elle nomme Scales. Quand celui-ci est capturé par un dragon adulte, elle se lance dans une quête épique et périlleuse à travers le monde pour le retrouver.",
            durationMinutes = 15,
            releaseYear = 2010,
            rating = 4.9f,
            posterUrl = "https://images.unsplash.com/photo-1514539079130-25950c84af65?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            category = "Animation",
            genres = listOf("Animation", "Fantastique", "Aventure"),
            director = "Colin Levy",
            cast = listOf("Halina Reijn", "Thom Hoffman"),
            language = "Français",
            licenseType = "Creative Commons BY 3.0 (Blender Foundation)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = true,
            isAwardWinner = true
        ),
        Movie(
            id = "elephants_dream",
            title = "Elephants Dream",
            originalTitle = "Elephants Dream (Orange Project)",
            synopsis = "Deux voyageurs explorent un univers surréaliste et infini composé d'étranges machines mécaniques, oscillant entre l'harmonie des rouages et le chaos d'une architecture vivante.",
            durationMinutes = 11,
            releaseYear = 2006,
            rating = 4.5f,
            posterUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            category = "Animation",
            genres = listOf("Animation", "Mystère", "Art"),
            director = "Bassam Kurdali",
            cast = listOf("Tygo Gernandt", "Cas Jansen"),
            language = "Anglais",
            licenseType = "Creative Commons BY 2.5 (Blender Foundation)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = false
        ),
        Movie(
            id = "night_living_dead",
            title = "La Nuit des morts-vivants",
            originalTitle = "Night of the Living Dead",
            synopsis = "Classique absolu du cinéma d'horreur de George A. Romero. Des morts inexplicablement ressuscités assiègent une ferme isolée de Pennsylvanie où se sont réfugiés plusieurs inconnus que la panique divise.",
            durationMinutes = 96,
            releaseYear = 1968,
            rating = 4.8f,
            posterUrl = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            category = "Classiques",
            genres = listOf("Horreur", "Classique", "Thriller"),
            director = "George A. Romero",
            cast = listOf("Duane Jones", "Judith O'Dea", "Karl Hardman", "Marilyn Eastman"),
            language = "Français",
            licenseType = "Domaine Public (1968 USA Copyright Failure)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = true,
            isAwardWinner = true
        ),
        Movie(
            id = "metropolis_1927",
            title = "Metropolis (Édition Restaurée)",
            originalTitle = "Metropolis",
            synopsis = "Chef-d'œuvre visionnaire de Fritz Lang. Dans une mégalopole du futur séparée entre les maîtres des hauteurs et les ouvriers des profondeurs, une jeune prophétesse tente de réconcilier la tête et les mains par le cœur.",
            durationMinutes = 153,
            releaseYear = 1927,
            rating = 4.9f,
            posterUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            category = "Classiques",
            genres = listOf("Classique", "Science-Fiction", "Drame"),
            director = "Fritz Lang",
            cast = listOf("Alfred Abel", "Gustav Fröhlich", "Brigitte Helm", "Rudolf Klein-Rogge"),
            language = "Muet / Musique",
            licenseType = "Domaine Public International",
            isDownloadable = true,
            isFeatured = true,
            isTrending = false,
            isAwardWinner = true
        ),
        Movie(
            id = "voyage_lune_1902",
            title = "Le Voyage dans la Lune",
            originalTitle = "A Trip to the Moon",
            synopsis = "Le premier film de science-fiction de l'histoire du cinéma, créé par le magicien Georges Méliès. Un groupe d'astronomes est propulsé vers la Lune dans une capsule tirée par un gigantesque canon.",
            durationMinutes = 14,
            releaseYear = 1902,
            rating = 4.9f,
            posterUrl = "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            category = "Classiques",
            genres = listOf("Classique", "Aventure", "Fantastique"),
            director = "Georges Méliès",
            cast = listOf("Georges Méliès", "Bleuette Bernon", "François Lallement"),
            language = "Français",
            licenseType = "Domaine Public Mondial",
            isDownloadable = true,
            isFeatured = false,
            isTrending = true,
            isAwardWinner = true
        ),
        Movie(
            id = "nasa_artemis",
            title = "NASA : En Route vers la Lune et Mars",
            originalTitle = "NASA Artemis: Path to the Moon & Beyond",
            synopsis = "Une immersion spectaculaire dans les coulisses de la mission Artemis de la NASA. Découvrez les technologies spatiales de nouvelle génération qui permettront aux humains de retourner durablement sur la Lune.",
            durationMinutes = 45,
            releaseYear = 2024,
            rating = 4.8f,
            posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            category = "Documentaire",
            genres = listOf("Documentaire", "Espace", "Science"),
            director = "NASA Media Operations",
            cast = listOf("Reid Wiseman", "Victor Glover", "Christina Koch", "Jeremy Hansen"),
            language = "Français",
            licenseType = "Domaine Public (Gouvernement fédéral USA / NASA)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = true,
            isAwardWinner = false
        ),
        Movie(
            id = "charade_1963",
            title = "Charade",
            originalTitle = "Charade",
            synopsis = "Regina Lampert découvre à son retour de vacances que son mari a été assassiné et que son compte en banque a été vidé. Elle est rapidement pourchassée dans Paris par trois criminels qui réclament une fortune volée.",
            durationMinutes = 113,
            releaseYear = 1963,
            rating = 4.7f,
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
            category = "Classiques",
            genres = listOf("Comédie", "Mystère", "Romance"),
            director = "Stanley Donen",
            cast = listOf("Cary Grant", "Audrey Hepburn", "Walter Matthau", "James Coburn"),
            language = "Français",
            licenseType = "Domaine Public (Notice de droit d'auteur défaillante)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = false,
            isAwardWinner = true
        ),
        Movie(
            id = "cosmos_laundromat",
            title = "Cosmos Laundromat",
            originalTitle = "Cosmos Laundromat (Gooseberry Project)",
            synopsis = "Sur une île désolée, Franck le mouton tente de mettre fin à ses jours jusqu'à ce qu'un mystérieux représentant nommé Victor lui propose d'explorer des vies infinies grâce à une laverie interdimensionnelle.",
            durationMinutes = 13,
            releaseYear = 2015,
            rating = 4.6f,
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            category = "Animation",
            genres = listOf("Animation", "Absurde", "Science-Fiction"),
            director = "Mathieu Auvray",
            cast = listOf("Pierre Bokma", "Reinout Scholten van Aschat"),
            language = "Anglais",
            licenseType = "Creative Commons BY 3.0 (Blender Foundation)",
            isDownloadable = true,
            isFeatured = false,
            isTrending = false,
            isAwardWinner = true
        )
    )

    private val categories: List<CategoryItem> = listOf(
        CategoryItem("all", "Tous les contenus", "L'intégralité du catalogue streaming légal", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800"),
        CategoryItem("Animation", "Animation & 3D", "Films d'animation open-source et créations indépendantes", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800"),
        CategoryItem("Classiques", "Classiques du Cinéma", "Les plus grands chefs-d'œuvre du domaine public", "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800"),
        CategoryItem("Science-Fiction", "Science-Fiction", "Futurs dystopiques, voyages spatiaux et cyberpunk", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800"),
        CategoryItem("Documentaire", "Documentaires", "Exploration spatiale, nature et culture sans frontières", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800")
    )

    private val upcomingList: List<UpcomingMovie> = listOf(
        UpcomingMovie(
            id = "upcoming_project_gold",
            title = "Projet Or : L'Odyssée Ouverte",
            expectedReleaseDate = "15 Décembre 2026",
            posterUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600",
            synopsis = "Le nouveau projet d'animation en open source par la communauté mondiale de créateurs 3D. Une épopée mythologique réalisée à 100% avec des outils libres.",
            category = "Animation",
            licenseNotice = "Creative Commons Attribution 4.0 International",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        ),
        UpcomingMovie(
            id = "upcoming_restoration_caligari",
            title = "Le Cabinet du Dr Caligari (Restauration 4K)",
            expectedReleaseDate = "28 Janvier 2027",
            posterUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800",
            backdropUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600",
            synopsis = "Une nouvelle numérisation en résolution 4K avec bande-son orchestrale originale du monument de l'expressionnisme allemand.",
            category = "Classiques",
            licenseNotice = "Domaine Public (1920)",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        UpcomingMovie(
            id = "upcoming_james_webb",
            title = "James Webb : Aux Frontières de l'Univers",
            expectedReleaseDate = "14 Février 2027",
            posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800",
            backdropUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1600",
            synopsis = "Documentaire officiel de la NASA retraçant les découvertes des premières galaxies formées après le Big Bang.",
            category = "Documentaire",
            licenseNotice = "Domaine Public Gouvernemental",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        )
    )

    fun getAllMovies(): List<Movie> = catalogue

    fun getFeaturedMovies(): List<Movie> = catalogue.filter { it.isFeatured }

    fun getTrendingMovies(): List<Movie> = catalogue.filter { it.isTrending }

    fun getMoviesByCategory(category: String): List<Movie> {
        if (category == "all" || category.isBlank()) return catalogue
        return catalogue.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun getMovieById(id: String): Movie? = catalogue.find { it.id == id }

    fun getCategories(): List<CategoryItem> = categories

    fun getUpcomingMovies(): List<UpcomingMovie> = upcomingList

    fun searchMovies(
        query: String,
        categoryFilter: String? = null,
        yearFilter: Int? = null,
        languageFilter: String? = null
    ): List<Movie> {
        return catalogue.filter { movie ->
            val matchesQuery = query.isBlank() ||
                    movie.title.contains(query, ignoreCase = true) ||
                    movie.originalTitle.contains(query, ignoreCase = true) ||
                    movie.synopsis.contains(query, ignoreCase = true) ||
                    movie.director.contains(query, ignoreCase = true) ||
                    movie.cast.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = categoryFilter.isNullOrBlank() ||
                    categoryFilter.equals("all", ignoreCase = true) ||
                    movie.category.equals(categoryFilter, ignoreCase = true)

            val matchesYear = yearFilter == null || movie.releaseYear == yearFilter

            val matchesLanguage = languageFilter.isNullOrBlank() ||
                    languageFilter.equals("Toutes", ignoreCase = true) ||
                    movie.language.contains(languageFilter, ignoreCase = true)

            matchesQuery && matchesCategory && matchesYear && matchesLanguage
        }
    }

    // Room Watch History delegates
    fun getAllHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllHistory()
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getContinueWatching()
    fun observeProgressForMovie(movieId: String): Flow<WatchHistoryEntity?> = watchHistoryDao.observeProgressForMovie(movieId)
    suspend fun getProgressForMovie(movieId: String): WatchHistoryEntity? = watchHistoryDao.getProgressForMovie(movieId)

    suspend fun saveProgress(movieId: String, progressMs: Long, durationMs: Long) {
        val isCompleted = durationMs > 0 && progressMs >= (durationMs * 0.95)
        watchHistoryDao.saveProgress(
            WatchHistoryEntity(
                movieId = movieId,
                progressMs = progressMs,
                durationMs = durationMs,
                lastWatchedTimestamp = System.currentTimeMillis(),
                isCompleted = isCompleted
            )
        )
    }

    suspend fun deleteHistory(movieId: String) = watchHistoryDao.deleteHistory(movieId)
    suspend fun clearAllHistory() = watchHistoryDao.clearAllHistory()

    // Room Favorites delegates
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    fun isFavorite(movieId: String): Flow<Boolean> = favoriteDao.isFavorite(movieId)

    suspend fun toggleFavorite(movieId: String, currentIsFav: Boolean) {
        if (currentIsFav) {
            favoriteDao.removeFavorite(movieId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(movieId = movieId))
        }
    }

    // Room Downloads delegates
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    fun observeDownload(movieId: String): Flow<DownloadEntity?> = downloadDao.observeDownload(movieId)

    suspend fun startDownload(movie: Movie) {
        val destFile = File(context.filesDir, "downloads/${movie.id}.mp4")
        destFile.parentFile?.mkdirs()
        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        val entity = DownloadEntity(
            movieId = movie.id,
            title = movie.title,
            posterUrl = movie.posterUrl,
            localFilePath = destFile.absolutePath,
            fileSizeBytes = (movie.durationMinutes * 15L * 1024 * 1024), // Realistic ~15MB/min
            downloadedBytes = (movie.durationMinutes * 15L * 1024 * 1024),
            status = "COMPLETED",
            progressPercent = 100,
            downloadedTimestamp = System.currentTimeMillis()
        )
        downloadDao.saveDownload(entity)
    }

    suspend fun deleteDownload(movieId: String) {
        val entity = downloadDao.getDownload(movieId)
        if (entity != null) {
            val file = File(entity.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.deleteDownload(movieId)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MovieRepository? = null

        fun getInstance(context: Context): MovieRepository {
            return INSTANCE ?: synchronized(this) {
                val db = GpnFlixDatabase.getDatabase(context)
                val instance = MovieRepository(
                    watchHistoryDao = db.watchHistoryDao(),
                    favoriteDao = db.favoriteDao(),
                    downloadDao = db.downloadDao(),
                    context = context.applicationContext
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
