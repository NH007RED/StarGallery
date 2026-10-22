package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stargallery.app.domain.Page
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.util.ApiResult
import com.stargallery.app.util.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val error: AppError) : LoadState<Nothing>
}

data class PagedState(
    val items: List<PersonSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
    val loadMoreError: AppError? = null,
    val hasMore: Boolean = true,
    val lastPage: Int = 0,
    val loadedOnce: Boolean = false
) {
    val isEmpty: Boolean get() = loadedOnce && items.isEmpty() && error == null && !isLoading
}

/**
 * Generic pagination helper. Dedupes by person id, never runs two loads at once,
 * and keeps trying the next page when a page yields no new items.
 */
class Paginator(
    private val scope: CoroutineScope,
    private var loader: suspend (page: Int) -> ApiResult<Page<PersonSummary>>
) {
    private val _state = MutableStateFlow(PagedState())
    val state: StateFlow<PagedState> = _state.asStateFlow()
    private var job: Job? = null

    /** Replace the data source and load the first page. */
    fun setSource(newLoader: suspend (page: Int) -> ApiResult<Page<PersonSummary>>) {
        loader = newLoader
        refresh()
    }

    fun refresh() {
        job?.cancel()
        _state.value = PagedState(isLoading = true)
        job = scope.launch { load(isFirst = true) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.hasMore || s.error != null) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        job = scope.launch { load(isFirst = false) }
    }

    private suspend fun load(isFirst: Boolean) {
        var attempts = 0
        while (attempts < MAX_EMPTY_SKIPS) {
            val page = _state.value.lastPage + 1
            when (val r = loader(page)) {
                is ApiResult.Failure -> {
                    _state.update {
                        if (isFirst) it.copy(isLoading = false, error = r.error, loadedOnce = true)
                        else it.copy(isLoadingMore = false, loadMoreError = r.error)
                    }
                    return
                }
                is ApiResult.Success -> {
                    var added = 0
                    _state.update { cur ->
                        val known = cur.items.mapTo(HashSet()) { it.id }
                        val fresh = r.data.items.filter { known.add(it.id) }
                        added = fresh.size
                        cur.copy(
                            items = cur.items + fresh,
                            lastPage = r.data.page,
                            hasMore = r.data.hasMore,
                            loadedOnce = true
                        )
                    }
                    if (added > 0 || !r.data.hasMore) break
                    attempts++
                }
            }
        }
        _state.update { it.copy(isLoading = false, isLoadingMore = false) }
    }

    private companion object {
        const val MAX_EMPTY_SKIPS = 3
    }
}

inline fun <reified T : ViewModel> viewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }
