package com.stargallery.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.domain.Industries
import com.stargallery.app.domain.Industry
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.ui.components.EmptyState
import com.stargallery.app.ui.components.ErrorState
import com.stargallery.app.ui.components.LoadMoreFooter
import com.stargallery.app.ui.components.PersonCard
import com.stargallery.app.ui.components.PersonCardSkeleton
import com.stargallery.app.viewmodel.DepartmentFilter
import com.stargallery.app.viewmodel.ExploreViewModel
import com.stargallery.app.viewmodel.SortOption

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    initialQuery: String?,
    initialIndustryId: String?,
    onPersonClick: (PersonSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val filters = ui.filters
    val paged = ui.paged
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    // Apply navigation arguments (from Home search / chips) once per distinct value.
    LaunchedEffect(initialQuery) { initialQuery?.let(viewModel::setQuery) }
    LaunchedEffect(initialIndustryId) {
        Industries.byId(initialIndustryId)?.let(viewModel::setIndustry)
    }

    // Lazy loading: request the next page when the user nears the end of the grid.
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, paged.items.size) {
        if (nearEnd) viewModel.loadMore()
    }
    // New source -> scroll back to top.
    LaunchedEffect(filters.query, filters.industry) { gridState.scrollToItem(0) }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // Search with clear + cancel
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search actresses") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search text")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            if (filters.query.isNotEmpty()) {
                TextButton(onClick = {
                    viewModel.clearQuery()
                    focusManager.clearFocus()
                }) { Text("Cancel") }
            }
        }

        // Filters: region, sort, department
        FilterRow(
            industry = filters.industry,
            sort = filters.sort,
            department = filters.department,
            searching = filters.query.isNotBlank(),
            onIndustry = viewModel::setIndustry,
            onSort = viewModel::setSort,
            onDepartment = viewModel::setDepartment
        )

        Box(Modifier.weight(1f)) {
            when {
                paged.isLoading && paged.items.isEmpty() -> SkeletonGrid()
                paged.error != null && paged.items.isEmpty() ->
                    ErrorState(paged.error, onRetry = viewModel::retry, modifier = Modifier.fillMaxSize())
                paged.isEmpty || (paged.items.isNotEmpty() && ui.visible.isEmpty() && !paged.hasMore) ->
                    EmptyState(
                        title = "No performers found",
                        message = when {
                            filters.query.isNotBlank() -> "No actresses match \"${filters.query.trim()}\". Check the spelling or try a different name."
                            paged.items.isNotEmpty() -> "Nothing matches the ${filters.department.label} filter."
                            else -> "TMDB returned no results for this selection."
                        },
                        actionLabel = if (filters.query.isNotBlank() || filters.industry != null || filters.department != DepartmentFilter.ALL) "Reset filters" else "Retry",
                        onAction = {
                            if (filters.query.isNotBlank() || filters.industry != null || filters.department != DepartmentFilter.ALL) {
                                viewModel.clearQuery(); viewModel.setIndustry(null); viewModel.setDepartment(DepartmentFilter.ALL)
                            } else viewModel.retry()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.visible, key = { it.id }) { person ->
                        PersonCard(person, onClick = { onPersonClick(person) })
                    }
                    if (paged.isLoadingMore || paged.loadMoreError != null) {
                        item(span = { GridItemSpan(2) }) {
                            LoadMoreFooter(paged.isLoadingMore, paged.loadMoreError, onRetry = viewModel::loadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(List(6) { it }) { PersonCardSkeleton() }
    }
}

@Composable
private fun FilterRow(
    industry: Industry?,
    sort: SortOption,
    department: DepartmentFilter,
    searching: Boolean,
    onIndustry: (Industry?) -> Unit,
    onSort: (SortOption) -> Unit,
    onDepartment: (DepartmentFilter) -> Unit
) {
    var regionOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box {
                    FilterChip(
                        selected = industry != null,
                        onClick = { regionOpen = true },
                        label = { Text(industry?.label ?: "Region / industry") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose region or industry") }
                    )
                    DropdownMenu(expanded = regionOpen, onDismissRequest = { regionOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("All (popular worldwide)") },
                            onClick = { onIndustry(null); regionOpen = false }
                        )
                        Industries.all.groupBy { it.group }.forEach { (group, list) ->
                            Text(
                                group.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            list.forEach { ind ->
                                DropdownMenuItem(
                                    text = { Text(ind.label) },
                                    onClick = { onIndustry(ind); regionOpen = false }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Box {
                    FilterChip(
                        selected = sort != SortOption.POPULARITY,
                        onClick = { sortOpen = true },
                        label = { Text("Sort: ${sort.label}") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose sort order") }
                    )
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { onSort(option); sortOpen = false }
                            )
                        }
                    }
                }
            }
            items(DepartmentFilter.entries.toList()) { d ->
                FilterChip(
                    selected = department == d,
                    onClick = { onDepartment(d) },
                    label = { Text(d.label) }
                )
            }
        }
        val note = when {
            searching && industry != null ->
                "Searching all of TMDB by name. Clear the search to browse ${industry.label}."
            industry != null ->
                "${industry.label}: performers credited in popular films of this industry. Not a statement of nationality."
            sort == SortOption.NAME ->
                "Sorting by name applies to the results loaded so far."
            else -> null
        }
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
}
