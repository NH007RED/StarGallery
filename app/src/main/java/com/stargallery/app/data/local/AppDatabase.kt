package com.stargallery.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [FavoriteEntity::class, RecentEntity::class, PersonCacheEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun personCacheDao(): PersonCacheDao

    companion object {
        const val VERSION = 1
        private const val NAME = "stargallery.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                // Favourites are user data, so we never fall back to destructive migration.
                .addMigrations(*Migrations.ALL)
                .build()
    }
}

/**
 * Schema migrations.
 *
 * Version 1 is the first released schema, so there is nothing to migrate yet.
 * When you change an entity:
 *   1. bump [AppDatabase.VERSION],
 *   2. add a Migration here (example below) and list it in [ALL],
 *   3. commit the new JSON from app/schemas/ so migrations can be tested.
 *
 * Example for a future version 2 that adds a column:
 *
 *   val MIGRATION_1_2 = object : Migration(1, 2) {
 *       override fun migrate(db: SupportSQLiteDatabase) {
 *           db.execSQL("ALTER TABLE favorites ADD COLUMN note TEXT")
 *       }
 *   }
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
