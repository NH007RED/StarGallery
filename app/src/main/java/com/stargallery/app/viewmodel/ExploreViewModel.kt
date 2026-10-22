package com.stargallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargallery.app.data.repository.DiscoveryRepository
import com.stargallery.app.domain.Industries
import com.stargallery.app.domain.Industry
import com.stargallery.app.domain.PersonSummary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    POPULARITY("Popularity"),
    NAME("Name (A–Z)")
}

enum class DepartmentFilter(val label: String, val tmdbValue: String?) {
    ALL("All", null),
    ACTING("Acting", "Acting"),
    DIRECTING("Directing", "Directing"),
    WRITING("Writing", "Writing"),
    PRODUCTION("Production", "Production")
}

data class ExploreFilters(
    val query: String = "",
    val industry: Industry? = null,
    val sort: SortOption = SortOption.POPULARITY,
    val department: DepartmentFilter = DepartmentFilter.ALL
)

data class ExploreUiState(
    val filters: ExploreFilters = ExploreFilters(),
    val paged: PagedState = PagedState(),
    /** Items after client-side sort + department filtering. */
    val visible: List<PersonSummary> = emptyList()
)

@OptIn(FlowPreview::class)
class ExploreViewModel(
    private val discovery: DiscoveryRepository,
    initialIndustryId: String? = null
) : ViewModel() {

    private val filters = MutableStateFlow(ExploreFilters(industry = Industries.byId(initialIndustryId)))
    private val paginator = Paginator(viewModelScope) { page -> discovery.popular(page) }

    val state: StateFlow<ExploreUiState> = combine(filters, paginator.state) { f, p ->
        val filtered = p.items.filter { f.department.tmdbValue == null || it.knownForDepartment == f.department.tmdbValue }
        val sorted = when (f.sort) {
            SortOption.POPULARITY -> filtered // server order is popularity-descending
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
        ExploreUiState(f, p, sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState(filters.value))

    init {
        // Source changes (query / industry) reload; sort and department are client-side only.
        viewModelScope.launch {
            filters
                .map { it.query.trim() to it.industry }
                .distinctUntilChanged()
                .debounce { (q, _) -> if (q.isEmpty()) 0L else 400L }
                .collect { (q, industry) -> applySource(q, industry) }
        }
    }

    private fun applySource(query: String, industry: Industry?) {
        paginator.setSource(
            when {
                query.isNotEmpty() -> { page -> discovery.search(query, page) }
                industry != null -> { page -> discovery.industry(industry, page) }
                else -> { page -> discovery.popular(page) }
            }
        )
    }

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun clearQuery() = filters.update { it.copy(query = "") }
    fun setIndustry(industry: Industry?) = filters.update { it.copy(industry = industry) }
    fun setSort(sort: SortOption) = filters.update { it.copy(sort = sort) }
    fun setDepartment(d: DepartmentFilter) = filters.update { it.copy(department = d) }
    fun loadMore() = paginator.loadMore()
    fun retry() = paginator.refresh()
}
