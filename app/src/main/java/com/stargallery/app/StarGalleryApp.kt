package com.stargallery.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.stargallery.app.data.local.AppDatabase
import com.stargallery.app.data.local.SettingsStore
import com.stargallery.app.data.remote.NetworkModule
import com.stargallery.app.data.remote.TmdbApi
import com.stargallery.app.data.repository.DiscoveryRepository
import com.stargallery.app.data.repository.LibraryRepository
import com.stargallery.app.data.repository.ProfileRepository

/** Simple manual dependency container (no DI framework needed for this app size). */
class AppContainer(app: Application, val imageLoader: ImageLoader) {
    val settingsStore = SettingsStore(app)
    private val database = AppDatabase.create(app)
    private val api: TmdbApi = NetworkModule.provideApi(app)

    val discovery = DiscoveryRepository(api, settingsStore)
    val profiles = ProfileRepository(api, database.personCacheDao(), settingsStore)
    val library = LibraryRepository(
        app, database.favoriteDao(), database.recentDao(), database.personCacheDao(), imageLoader
    )
}

class StarGalleryApp : Application(), ImageLoaderFactory {

    private val loader: ImageLoader by lazy {
        ImageLoader.Builder(this)
            .crossfade(300)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // TMDB image files are immutable per path
            .build()
    }

    val container: AppContainer by lazy { AppContainer(this, loader) }

    override fun newImageLoader(): ImageLoader = loader
}
