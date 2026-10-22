package com.stargallery.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun observeIsFavorite(id: Int): Flow<Boolean>

    @Query("SELECT id FROM favorites")
    fun observeIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_persons ORDER BY viewed_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentEntity)

    @Query(
        "DELETE FROM recent_persons WHERE id NOT IN " +
            "(SELECT id FROM recent_persons ORDER BY viewed_at DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)

    @Query("DELETE FROM recent_persons")
    suspend fun clear()
}

@Dao
interface PersonCacheDao {
    @Query("SELECT * FROM person_cache WHERE id = :id")
    suspend fun get(id: Int): PersonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PersonCacheEntity)

    @Query("SELECT COUNT(*) FROM person_cache")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM person_cache")
    suspend fun clear()
}
