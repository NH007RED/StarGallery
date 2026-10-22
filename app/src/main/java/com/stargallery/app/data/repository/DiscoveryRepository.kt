package com.stargallery.app.data.repository

import com.stargallery.app.data.local.SettingsStore
import com.stargallery.app.data.model.PersonDto
import com.stargallery.app.data.model.PersonListResponse
import com.stargallery.app.data.remote.TmdbApi
import com.stargallery.app.data.remote.TmdbConfig
import com.stargallery.app.domain.Industry
import com.stargallery.app.domain.Page
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.util.ApiResult
import com.stargallery.app.util.AppError
import com.stargallery.app.util.safeApiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Discovery: popular, trending, search and industry exploration.
 *
 * TMDB marks gender as 1 = female. We keep only gender == 1 for "actress" lists.
 * People with gender 0 (not specified) are excluded, so results can be incomplete.
 */
class DiscoveryRepository(
    private val api: TmdbApi,
    private val settings: SettingsStore
) {
    private suspend fun language() = settings.settings.first().language

    private fun <T> notConfigured(): ApiResult<T>? =
        if (TmdbConfig.isConfigured) null else ApiResult.Failure(AppError.NotConfigured)

    suspend fun popular(page: Int): ApiResult<Page<PersonSummary>> {
        notConfigured<Page<PersonSummary>>()?.let { return it }
        val lang = language()
        return collectFemale(page) { p -> api.popularPeople(lang, p) }
    }

    suspend fun trending(page: Int = 1): ApiResult<Page<PersonSummary>> {
        notConfigured<Page<PersonSummary>>()?.let { return it }
        val lang = language()
        return collectFemale(page) { p -> api.trendingPeople("week", lang, p) }
    }

    suspend fun search(query: String, page: Int): ApiResult<Page<PersonSummary>> {
        notConfigured<Page<PersonSummary>>()?.let { return it }
        val q = query.trim()
        if (q.isEmpty()) return ApiResult.Success(Page(emptyList(), page, false))
        val lang = language()
        return collectFemale(page) { p -> api.searchPeople(q, lang, p, false) }
    }

    /**
     * Performers credited in the cast of popular films matching an industry filter.
     * One call to /discover/movie plus one /movie/{id}/credits per film (max [FILMS_PER_PAGE]).
     * This is an association with the industry's films, not a nationality claim.
     */
    suspend fun industry(industry: Industry, page: Int): ApiResult<Page<PersonSummary>> {
        notConfigured<Page<PersonSummary>>()?.let { return it }
        val lang = language()

        val discover = safeApiCall {
            api.discoverMovies(
                language = lang,
                page = page,
                sortBy = "popularity.desc",
                originalLanguage = industry.originalLanguage,
                originCountry = industry.originCountry,
                includeAdult = false,
                includeVideo = false,
                minVotes = 20
            )
        }
        val response = when (discover) {
            is ApiResult.Failure -> return discover
            is ApiResult.Success -> discover.data
        }

        val films = response.results.take(FILMS_PER_PAGE)
        val credits = coroutineScope {
            films.map { film ->
                async { film to safeApiCall { api.movieCast(film.id, lang) } }
            }.awaitAll()
        }

        // If every credits call failed, surface the failure instead of an empty page.
        if (films.isNotEmpty() && credits.all { it.second is ApiResult.Failure }) {
            return (credits.first().second as ApiResult.Failure)
        }

        val seen = LinkedHashMap<Int, PersonSummary>()
        for ((film, result) in credits) {
            if (result !is ApiResult.Success) continue
            result.data.cast
                .filter { it.gender == 1 && (it.order ?: Int.MAX_VALUE) < 8 && !it.name.isNullOrBlank() }
                .forEach { member ->
                    seen.getOrPut(member.id) {
                        PersonSummary(
                            id = member.id,
                            name = member.name.orEmpty(),
                            profilePath = member.profilePath,
                            knownForDepartment = member.knownForDepartment,
                            popularity = member.popularity ?: 0.0,
                            knownFor = listOfNotNull(film.title)
                        )
                    }
                }
        }
        return ApiResult.Success(
            Page(
                items = seen.values.toList(),
                page = response.page,
                hasMore = response.page < response.totalPages && response.page < MAX_DISCOVER_PAGES
            )
        )
    }

    /**
     * TMDB person lists mix genders. To return a useful page of actresses we read up to
     * [MAX_PAGES_PER_REQUEST] consecutive TMDB pages until we have at least [MIN_ITEMS] results.
     */
    private suspend fun collectFemale(
        startPage: Int,
        fetch: suspend (Int) -> PersonListResponse
    ): ApiResult<Page<PersonSummary>> {
        val items = ArrayList<PersonSummary>()
        var next = startPage
        var lastFetched = startPage - 1
        var hasMore = true
        var fetched = 0
        while (hasMore && fetched < MAX_PAGES_PER_REQUEST && items.size < MIN_ITEMS) {
            when (val r = safeApiCall { fetch(next) }) {
                is ApiResult.Failure -> {
                    // Keep what we have if at least one page succeeded.
                    if (fetched == 0) return r
                    return ApiResult.Success(Page(items, lastFetched, true))
                }
                is ApiResult.Success -> {
                    items += r.data.results.filter { it.gender == 1 }.mapNotNull { it.toSummary() }
                    lastFetched = r.data.page
                    hasMore = r.data.page < r.data.totalPages
                    next = lastFetched + 1
                    fetched++
                }
            }
        }
        return ApiResult.Success(Page(items, lastFetched, hasMore))
    }

    private fun PersonDto.toSummary(): PersonSummary? {
        val n = name?.takeIf { it.isNotBlank() } ?: return null
        return PersonSummary(
            id = id,
            name = n,
            profilePath = profilePath,
            knownForDepartment = knownForDepartment,
            popularity = popularity ?: 0.0,
            knownFor = knownFor.orEmpty().mapNotNull { it.title ?: it.name }
        )
    }

    private companion object {
        const val FILMS_PER_PAGE = 8
        const val MAX_DISCOVER_PAGES = 20
        const val MAX_PAGES_PER_REQUEST = 3
        const val MIN_ITEMS = 8
    }
}
