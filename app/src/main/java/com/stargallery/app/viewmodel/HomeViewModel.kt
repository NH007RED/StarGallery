package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargallery.app.data.repository.DiscoveryRepository
import com.stargallery.app.data.repository.LibraryRepository
import com.stargallery.app.domain.Industry
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val discovery: DiscoveryRepository,
    library: LibraryRepository
) : ViewModel() {

    private val _trending = MutableStateFlow<LoadState<List<PersonSummary>>>(LoadState.Loading)
    val trending: StateFlow<LoadState<List<PersonSummary>>> = _trending

    private val _popular = MutableStateFlow<LoadState<List<PersonSummary>>>(LoadState.Loading)
    val popular: StateFlow<LoadState<List<PersonSummary>>> = _popular

    private val _industries = MutableStateFlow<Map<String, LoadState<List<PersonSummary>>>>(emptyMap())
    val industries: StateFlow<Map<String, LoadState<List<PersonSummary>>>> = _industries

    val recents: StateFlow<List<PersonSummary>> = library.recentPeople(12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        _trending.value = LoadState.Loading
        _popular.value = LoadState.Loading
        viewModelScope.launch {
            _trending.value = discovery.trending().toLoadState()
        }
        viewModelScope.launch {
            _popular.value = discovery.popular(1).toLoadState()
        }
    }

    /** Industry rows are loaded lazily when they scroll into view (each costs 1 + 8 requests). */
    fun ensureIndustry(industry: Industry, force: Boolean = false) {
        val current = _industries.value[industry.id]
        if (!force && current != null && current !is LoadState.Error) return
        _industries.update { it + (industry.id to LoadState.Loading) }
        viewModelScope.launch {
            val result = discovery.industry(industry, 1).toLoadState()
            _industries.update { it + (industry.id to result) }
        }
    }

    private fun ApiResult<com.stargallery.app.domain.Page<PersonSummary>>.toLoadState():
        LoadState<List<PersonSummary>> = when (this) {
        is ApiResult.Success -> LoadState.Success(data.items)
        is ApiResult.Failure -> LoadState.Error(error)
    }
}
