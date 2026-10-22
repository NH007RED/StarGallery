package com.stargallery.app.data.model

import com.google.gson.annotations.SerializedName

/** Typed models for the TMDB v3 responses used by the app. All fields are nullable-tolerant. */

data class PersonListResponse(
    val page: Int = 1,
    val results: List<PersonDto> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int = 1,
    @SerializedName("total_results") val totalResults: Int = 0
)

data class PersonDto(
    val id: Int,
    val name: String? = null,
    val gender: Int? = null, // TMDB: 0 unknown, 1 female, 2 male, 3 non-binary
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Double? = null,
    @SerializedName("known_for") val knownFor: List<KnownForDto>? = null
)

data class KnownForDto(
    val id: Int? = null,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("media_type") val mediaType: String? = null
)

data class PersonDetailDto(
    val id: Int,
    val name: String? = null,
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val gender: Int? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Double? = null,
    @SerializedName("also_known_as") val alsoKnownAs: List<String>? = null,
    @SerializedName("imdb_id") val imdbId: String? = null,
    val homepage: String? = null
)

data class PersonImagesResponse(
    val id: Int = 0,
    val profiles: List<ImageDto> = emptyList()
)

data class ImageDto(
    @SerializedName("file_path") val filePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerializedName("aspect_ratio") val aspectRatio: Double? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null
)

data class MovieCreditsResponse(
    val id: Int = 0,
    val cast: List<MovieCastDto> = emptyList()
)

data class MovieCastDto(
    val id: Int,
    val title: String? = null,
    val character: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    val popularity: Double? = null
)

data class DiscoverMovieResponse(
    val page: Int = 1,
    val results: List<MovieDto> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int = 1
)

data class MovieDto(
    val id: Int,
    val title: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    val popularity: Double? = null
)

/** Response of /movie/{id}/credits (cast of a film). */
data class CastListResponse(
    val id: Int = 0,
    val cast: List<CastMemberDto> = emptyList()
)

data class CastMemberDto(
    val id: Int,
    val name: String? = null,
    val gender: Int? = null,
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("known_for_department") val knownForDepartment: String? = null,
    val popularity: Double? = null,
    val order: Int? = null
)
