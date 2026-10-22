package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargallery.app.data.repository.LibraryRepository
import com.stargallery.app.domain.PersonSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val query: String = "",
    val all: List<PersonSummary> = emptyList(),
    val visible: List<PersonSummary> = emptyList()
)

class FavoritesViewModel(private val library: LibraryRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<FavoritesUiState> = combine(library.favoritePeople, query) { all, q ->
        val needle = q.trim()
        FavoritesUiState(
            query = q,
            all = all,
            visible = if (needle.isEmpty()) all else all.filter { it.name.contains(needle, ignoreCase = true) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun setQuery(q: String) { query.value = q }

    fun remove(id: Int) {
        viewModelScope.launch { library.removeFavorite(id) }
    }

    fun undoRemove(person: PersonSummary) {
        viewModelScope.launch { library.addFavorite(person) }
    }
}
