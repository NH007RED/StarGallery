package com.stargallery.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "profile_path") val profilePath: String?,
    @ColumnInfo(name = "known_for_department") val knownForDepartment: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

@Entity(tableName = "recent_persons")
data class RecentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "profile_path") val profilePath: String?,
    @ColumnInfo(name = "known_for_department") val knownForDepartment: String?,
    @ColumnInfo(name = "viewed_at") val viewedAt: Long
)

/** Offline cache of a full profile: details plus JSON-encoded filmography and image summaries. */
@Entity(tableName = "person_cache")
data class PersonCacheEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    @ColumnInfo(name = "place_of_birth") val placeOfBirth: String?,
    @ColumnInfo(name = "profile_path") val profilePath: String?,
    @ColumnInfo(name = "known_for_department") val knownForDepartment: String?,
    val popularity: Double,
    @ColumnInfo(name = "also_known_as") val alsoKnownAs: String,
    @ColumnInfo(name = "imdb_id") val imdbId: String?,
    @ColumnInfo(name = "filmography_json") val filmographyJson: String,
    @ColumnInfo(name = "images_json") val imagesJson: String,
    /** Language the biography/titles were requested in, so a language change refreshes the cache. */
    val language: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)
