package com.stargallery.app.domain

/** Lightweight performer used in lists, grids and carousels. */
data class PersonSummary(
    val id: Int,
    val name: String,
    val profilePath: String?,
    val knownForDepartment: String?,
    val popularity: Double,
    /** Titles TMDB lists as "known for" (or the film this person was found through). */
    val knownFor: List<String> = emptyList()
)

data class FilmCredit(
    val movieId: Int,
    val title: String,
    val character: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val voteAverage: Double?,
    val popularity: Double
) {
    /** Four-digit year, or null if TMDB has no release date. */
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

data class ProfileImage(
    val filePath: String,
    val width: Int,
    val height: Int
)

data class PersonDetails(
    val id: Int,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val knownForDepartment: String?,
    val popularity: Double,
    val alsoKnownAs: List<String>,
    val imdbId: String?
)

/** Everything the profile screen shows. */
data class PersonProfile(
    val details: PersonDetails,
    val filmography: List<FilmCredit>,
    val images: List<ProfileImage>,
    /** True when the data came from the local cache instead of a fresh network response. */
    val fromCache: Boolean,
    val cachedAt: Long?,
    /** True when filmography/images could not be refreshed even though details loaded. */
    val partial: Boolean
) {
    val knownForMovies: List<FilmCredit>
        get() = filmography.filter { it.posterPath != null }
            .sortedByDescending { it.popularity }
            .take(10)
}

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val hasMore: Boolean
)
