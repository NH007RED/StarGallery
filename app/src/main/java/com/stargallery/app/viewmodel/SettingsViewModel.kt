package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargallery.app.data.local.AppSettings
import com.stargallery.app.data.local.ImageQuality
import com.stargallery.app.data.local.SettingsStore
import com.stargallery.app.data.local.ThemeMode
import com.stargallery.app.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore,
    private val library: LibraryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val cachedProfiles: StateFlow<Int> = library.cachedProfileCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _imageCacheBytes = MutableStateFlow(0L)
    val imageCacheBytes: StateFlow<Long> = _imageCacheBytes

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch { _imageCacheBytes.value = library.imageAndHttpCacheSize() }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setTheme(mode) }
    fun setLanguage(code: String) = viewModelScope.launch { store.setLanguage(code) }
    fun setImageQuality(q: ImageQuality) = viewModelScope.launch { store.setImageQuality(q) }
    fun setCacheTtlHours(h: Int) = viewModelScope.launch { store.setCacheTtlHours(h) }

    fun clearImageCache() = viewModelScope.launch {
        library.clearImageAndHttpCache()
        refreshCacheSize()
        _message.value = "Image cache cleared"
    }

    fun clearProfileCache() = viewModelScope.launch {
        library.clearProfileCache()
        _message.value = "Offline profile cache cleared (favourites kept)"
    }

    fun clearRecents() = viewModelScope.launch {
        library.clearRecents()
        _message.value = "Recently viewed cleared"
    }

    fun messageShown() { _message.value = null }
}
