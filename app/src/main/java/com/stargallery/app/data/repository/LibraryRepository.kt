package com.stargallery.app.data.repository

import android.content.Context
import coil.ImageLoader
import com.stargallery.app.data.local.FavoriteDao
import com.stargallery.app.data.local.FavoriteEntity
import com.stargallery.app.data.local.PersonCacheDao
import com.stargallery.app.data.local.RecentDao
import com.stargallery.app.data.local.RecentEntity
import com.stargallery.app.domain.PersonSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/** Favourites, recently viewed history and cache management (all local). */
class LibraryRepository(
    private val context: Context,
    private val favorites: FavoriteDao,
    private val recents: RecentDao,
    private val personCache: PersonCacheDao,
    private val imageLoader: ImageLoader
) {
    val favoritePeople: Flow<List<PersonSummary>> =
        favorites.observeAll().map { list -> list.map { it.toSummary() } }

    val favoriteIds: Flow<Set<Int>> = favorites.observeIds().map { it.toSet() }

    fun recentPeople(limit: Int = MAX_RECENTS): Flow<List<PersonSummary>> =
        recents.observeRecent(limit).map { list -> list.map { it.toSummary() } }

    fun isFavorite(id: Int): Flow<Boolean> = favorites.observeIsFavorite(id)

    val cachedProfileCount: Flow<Int> = personCache.observeCount()

    suspend fun addFavorite(p: PersonSummary) = favorites.upsert(
        FavoriteEntity(p.id, p.name, p.profilePath, p.knownForDepartment, System.currentTimeMillis())
    )

    suspend fun removeFavorite(id: Int) = favorites.delete(id)

    suspend fun toggleFavorite(p: PersonSummary, currentlyFavorite: Boolean) {
        if (currentlyFavorite) removeFavorite(p.id) else addFavorite(p)
    }

    suspend fun recordView(p: PersonSummary) {
        recents.upsert(
            RecentEntity(p.id, p.name, p.profilePath, p.knownForDepartment, System.currentTimeMillis())
        )
        recents.trim(MAX_RECENTS)
    }

    suspend fun clearRecents() = recents.clear()

    suspend fun clearProfileCache() = personCache.clear()

    /** Clears Coil's memory + disk caches and the HTTP cache. Favourites are never touched. */
    suspend fun clearImageAndHttpCache() = withContext(Dispatchers.IO) {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        File(context.cacheDir, "http_cache").deleteRecursively()
        Unit
    }

    /** Approximate size of image + HTTP caches in bytes. */
    suspend fun imageAndHttpCacheSize(): Long = withContext(Dispatchers.IO) {
        val disk = imageLoader.diskCache?.size ?: 0L
        val http = File(context.cacheDir, "http_cache").walkTopDown().filter { it.isFile }.sumOf { it.length() }
        disk + http
    }

    private fun FavoriteEntity.toSummary() = PersonSummary(id, name, profilePath, knownForDepartment, 0.0)
    private fun RecentEntity.toSummary() = PersonSummary(id, name, profilePath, knownForDepartment, 0.0)

    companion object {
        const val MAX_RECENTS = 30
    }
}
