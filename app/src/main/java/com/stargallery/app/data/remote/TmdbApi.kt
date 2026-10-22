package com.stargallery.app.data.remote

import com.stargallery.app.data.model.CastListResponse
import com.stargallery.app.data.model.DiscoverMovieResponse
import com.stargallery.app.data.model.MovieCreditsResponse
import com.stargallery.app.data.model.PersonDetailDto
import com.stargallery.app.data.model.PersonImagesResponse
import com.stargallery.app.data.model.PersonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB v3 endpoints used by StarGallery. Nullable query parameters are omitted when null.
 * Docs: https://developer.themoviedb.org/reference
 */
interface TmdbApi {

    @GET("person/popular")
    suspend fun popularPeople(
        @Query("language") language: String,
        @Query("page") page: Int
    ): PersonListResponse

    @GET("trending/person/{window}")
    suspend fun trendingPeople(
        @Path("window") window: String,
        @Query("language") language: String,
        @Query("page") page: Int
    ): PersonListResponse

    @GET("search/person")
    suspend fun searchPeople(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean
    ): PersonListResponse

    @GET("person/{id}")
    suspend fun personDetails(
        @Path("id") id: Int,
        @Query("language") language: String
    ): PersonDetailDto

    @GET("person/{id}/images")
    suspend fun personImages(@Path("id") id: Int): PersonImagesResponse

    @GET("person/{id}/movie_credits")
    suspend fun personMovieCredits(
        @Path("id") id: Int,
        @Query("language") language: String
    ): MovieCreditsResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("sort_by") sortBy: String,
        @Query("with_original_language") originalLanguage: String?,
        @Query("with_origin_country") originCountry: String?,
        @Query("include_adult") includeAdult: Boolean,
        @Query("include_video") includeVideo: Boolean,
        @Query("vote_count.gte") minVotes: Int
    ): DiscoverMovieResponse

    /** Cast list of a film (used to find performers associated with a cinema industry). */
    @GET("movie/{id}/credits")
    suspend fun movieCast(
        @Path("id") movieId: Int,
        @Query("language") language: String
    ): CastListResponse
}
