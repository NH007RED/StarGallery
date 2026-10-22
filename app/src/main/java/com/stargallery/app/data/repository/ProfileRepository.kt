package com.stargallery.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stargallery.app.data.local.PersonCacheDao
import com.stargallery.app.data.local.PersonCacheEntity
import com.stargallery.app.data.local.SettingsStore
import com.stargallery.app.data.remote.TmdbApi
import com.stargallery.app.data.remote.TmdbConfig
import com.stargallery.app.domain.FilmCredit
import com.stargallery.app.domain.PersonDetails
import com.stargallery.app.domain.PersonProfile
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.domain.ProfileImage
import com.stargallery.app.util.ApiResult
import com.stargallery.app.util.AppError
import com.stargallery.app.util.safeApiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Loads full profiles with an offline cache.
 *
 * Policy: fresh cache (younger than the TTL, same language) is returned without a network call
 * unless [forceRefresh]. If the network fails and any cached copy exists, the cached copy is
 * returned with `fromCache = true` so the user can still read it offline.
 */
class ProfileRepository(
    private val api: TmdbApi,
    private val cache: PersonCacheDao,
    private val settings: SettingsStore,
    private val gson: Gson = Gson()
) {
    suspend fun loadProfile(id: Int, forceRefresh: Boolean = false): ApiResult<PersonProfile> {
        val s = settings.settings.first()
        val cached = cache.get(id)
        val now = System.currentTimeMillis()
        val ttlMs = s.cacheTtlHours * 60L * 60L * 1000L
        val cachedFresh = cached != null && cached.language == s.language && (now - cached.cachedAt) < ttlMs

        if (!forceRefresh && cachedFresh && cached != null) {
            return ApiResult.Success(cached.toProfile(partial = false))
        }

        if (!TmdbConfig.isConfigured) {
            return if (cached != null) ApiResult.Success(cached.toProfile(partial = false))
            else ApiResult.Failure(AppError.NotConfigured)
        }

        val network = fetchFromNetwork(id, s.language)
        return when (network) {
            is ApiResult.Success -> {
                cache.upsert(network.data.toEntity(s.language, now))
                ApiResult.Success(network.data.copy(fromCache = false, cachedAt = now))
            }
            is ApiResult.Failure ->
                if (cached != null) ApiResult.Success(cached.toProfile(partial = false))
                else network
        }
    }

    private suspend fun fetchFromNetwork(id: Int, language: String): ApiResult<PersonProfile> = coroutineScope {
        val detailsDeferred = async { safeApiCall { api.personDetails(id, language) } }
        val creditsDeferred = async { safeApiCall { api.personMovieCredits(id, language) } }
        val imagesDeferred = async { safeApiCall { api.personImages(id) } }
        val detailsRes = detailsDeferred.await()
        val creditsRes = creditsDeferred.await()
        val imagesRes = imagesDeferred.await()

        val dto = when (detailsRes) {
            is ApiResult.Failure -> return@coroutineScope detailsRes
            is ApiResult.Success -> detailsRes.data
        }

        val filmography = (creditsRes as? ApiResult.Success)?.data?.cast.orEmpty().map {
            FilmCredit(
                movieId = it.id,
                title = it.title.orEmpty().ifBlank { "Untitled" },
                character = it.character?.takeIf { c -> c.isNotBlank() },
                releaseDate = it.releaseDate?.takeIf { d -> d.isNotBlank() },
                posterPath = it.posterPath,
                voteAverage = it.voteAverage,
                popularity = it.popularity ?: 0.0
            )
        }

        val images = (imagesRes as? ApiResult.Success)?.data?.profiles.orEmpty().mapNotNull { img ->
            img.filePath?.let { ProfileImage(it, img.width ?: 0, img.height ?: 0) }
        }

        val partial = creditsRes is ApiResult.Failure || imagesRes is ApiResult.Failure

        ApiResult.Success(
            PersonProfile(
                details = PersonDetails(
                    id = dto.id,
                    name = dto.name.orEmpty().ifBlank { "Unknown" },
                    biography = dto.biography?.takeIf { it.isNotBlank() },
                    birthday = dto.birthday?.takeIf { it.isNotBlank() },
                    deathday = dto.deathday?.takeIf { it.isNotBlank() },
                    placeOfBirth = dto.placeOfBirth?.takeIf { it.isNotBlank() },
                    profilePath = dto.profilePath,
                    knownForDepartment = dto.knownForDepartment,
                    popularity = dto.popularity ?: 0.0,
                    alsoKnownAs = dto.alsoKnownAs.orEmpty(),
                    imdbId = dto.imdbId?.takeIf { it.isNotBlank() }
                ),
                filmography = filmography,
                images = images,
                fromCache = false,
                cachedAt = null,
                partial = partial
            )
        )
    }

    /**
     * Related performers: women billed in the casts of this person's most popular films.
     * Not cached. Returns an empty list on any failure (the section simply hides).
     */
    suspend fun related(profile: PersonProfile): List<PersonSummary> {
        if (!TmdbConfig.isConfigured) return emptyList()
        val language = settings.settings.first().language
        val films = profile.knownForMovies.take(4)
        if (films.isEmpty()) return emptyList()
        val casts = coroutineScope {
            films.map { film ->
                async { film to safeApiCall { api.movieCast(film.movieId, language) } }
            }.awaitAll()
        }
        val seen = LinkedHashMap<Int, PersonSummary>()
        for ((film, res) in casts) {
            if (res !is ApiResult.Success) continue
            res.data.cast
                .filter {
                    it.gender == 1 && it.id != profile.details.id &&
                        (it.order ?: Int.MAX_VALUE) < 10 && !it.name.isNullOrBlank()
                }
                .forEach { m ->
                    seen.getOrPut(m.id) {
                        PersonSummary(
                            id = m.id,
                            name = m.name.orEmpty(),
                            profilePath = m.profilePath,
                            knownForDepartment = m.knownForDepartment,
                            popularity = m.popularity ?: 0.0,
                            knownFor = listOf(film.title)
                        )
                    }
                }
        }
        return seen.values.sortedByDescending { it.popularity }.take(15)
    }

    private fun PersonCacheEntity.toProfile(partial: Boolean): PersonProfile {
        val filmType = object : TypeToken<List<FilmCredit>>() {}.type
        val imageType = object : TypeToken<List<ProfileImage>>() {}.type
        val films: List<FilmCredit> = runCatching { gson.fromJson<List<FilmCredit>>(filmographyJson, filmType) }
            .getOrNull().orEmpty()
        val imgs: List<ProfileImage> = runCatching { gson.fromJson<List<ProfileImage>>(imagesJson, imageType) }
            .getOrNull().orEmpty()
        return PersonProfile(
            details = PersonDetails(
                id = id,
                name = name,
                biography = biography,
                birthday = birthday,
                deathday = deathday,
                placeOfBirth = placeOfBirth,
                profilePath = profilePath,
                knownForDepartment = knownForDepartment,
                popularity = popularity,
                alsoKnownAs = alsoKnownAs.split("\u001F").filter { it.isNotBlank() },
                imdbId = imdbId
            ),
            filmography = films,
            images = imgs,
            fromCache = true,
            cachedAt = cachedAt,
            partial = partial
        )
    }

    private fun PersonProfile.toEntity(language: String, now: Long) = PersonCacheEntity(
        id = details.id,
        name = details.name,
        biography = details.biography,
        birthday = details.birthday,
        deathday = details.deathday,
        placeOfBirth = details.placeOfBirth,
        profilePath = details.profilePath,
        knownForDepartment = details.knownForDepartment,
        popularity = details.popularity,
        alsoKnownAs = details.alsoKnownAs.joinToString("\u001F"),
        imdbId = details.imdbId,
        filmographyJson = gson.toJson(filmography),
        imagesJson = gson.toJson(images),
        language = language,
        cachedAt = now
    )
}
