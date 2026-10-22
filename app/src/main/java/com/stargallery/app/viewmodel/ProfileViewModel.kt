package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargallery.app.data.repository.LibraryRepository
import com.stargallery.app.data.repository.ProfileRepository
import com.stargallery.app.domain.PersonProfile
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.util.ApiResult
import com.stargallery.app.util.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: PersonProfile? = null,
    val error: AppError? = null,
    val related: List<PersonSummary> = emptyList(),
    val isRefreshing: Boolean = false
)

class ProfileViewModel(
    private val personId: Int,
    private val profiles: ProfileRepository,
    private val library: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    val isFavorite: StateFlow<Boolean> = library.isFavorite(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load(force = false)
    }

    fun retry() = load(force = true)

    private fun load(force: Boolean) {
        _state.update {
            if (it.profile == null) it.copy(isLoading = true, error = null)
            else it.copy(isRefreshing = true, error = null)
        }
        viewModelScope.launch {
            when (val r = profiles.loadProfile(personId, forceRefresh = force)) {
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, error = if (it.profile == null) r.error else null)
                }
                is ApiResult.Success -> {
                    val profile = r.data
                    _state.update { it.copy(isLoading = false, isRefreshing = false, profile = profile, error = null) }
                    library.recordView(profile.toSummary())
                    // Related performers are best-effort and loaded after the main content shows.
                    if (!profile.fromCache) {
                        val related = profiles.related(profile)
                        _state.update { it.copy(related = related) }
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            library.toggleFavorite(profile.toSummary(), isFavorite.value)
        }
    }

    private fun PersonProfile.toSummary() = PersonSummary(
        id = details.id,
        name = details.name,
        profilePath = details.profilePath,
        knownForDepartment = details.knownForDepartment,
        popularity = details.popularity
    )
}
