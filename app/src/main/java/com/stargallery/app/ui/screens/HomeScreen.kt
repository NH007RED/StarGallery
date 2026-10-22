package com.stargallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.domain.Industries
import com.stargallery.app.domain.Industry
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.ui.components.CarouselSkeleton
import com.stargallery.app.ui.components.ErrorState
import com.stargallery.app.ui.components.PersonCard
import com.stargallery.app.ui.components.PersonCardSkeleton
import com.stargallery.app.ui.components.PersonCarousel
import com.stargallery.app.ui.components.RemoteImage
import com.stargallery.app.ui.components.SectionHeader
import com.stargallery.app.ui.components.bottomScrim
import com.stargallery.app.ui.theme.SGColors
import com.stargallery.app.util.ImageKind
import com.stargallery.app.viewmodel.HomeViewModel
import com.stargallery.app.viewmodel.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPersonClick: (PersonSummary) -> Unit,
    onSearch: (String) -> Unit,
    onIndustryClick: (Industry) -> Unit,
    onSeeAllPopular: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val popular by viewModel.popular.collectAsStateWithLifecycle()
    val industries by viewModel.industries.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero
        item(key = "hero") {
            HeroSection(
                state = trending,
                onPersonClick = onPersonClick,
                onRetry = viewModel::refresh
            )
        }

        // Search bar
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Search actresses by name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query.trim()) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Category chips
        item(key = "chips") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Industries.homeChips, key = { it.id }) { industry ->
                    AssistChip(
                        onClick = { onIndustryClick(industry) },
                        label = { Text(industry.label) }
                    )
                }
            }
        }

        // Trending carousel
        item(key = "trending") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Trending this week")
                when (val s = trending) {
                    LoadState.Loading -> CarouselSkeleton()
                    is LoadState.Error -> ErrorState(s.error, onRetry = viewModel::refresh)
                    is LoadState.Success ->
                        if (s.data.isEmpty()) {
                            Text(
                                "No trending performers right now.",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else PersonCarousel(s.data, onPersonClick)
                }
            }
        }

        // Popular grid (first page preview; full list lives in Explore)
        item(key = "popular") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Popular actresses", actionLabel = "See all", onAction = onSeeAllPopular)
                when (val s = popular) {
                    LoadState.Loading -> PopularGrid(emptyList(), loading = true, onPersonClick = onPersonClick)
                    is LoadState.Error -> ErrorState(s.error, onRetry = viewModel::refresh)
                    is LoadState.Success -> PopularGrid(s.data.take(6), loading = false, onPersonClick = onPersonClick)
                }
            }
        }

        // International cinema
        item(key = "intl-header") {
            SectionHeader(
                title = "International cinema",
                subtitle = "Performers credited in popular films of each industry. This is an association with the films, not a statement of nationality."
            )
        }
        items(Industries.homeSections, key = { "intl-${it.id}" }) { industry ->
            LaunchedEffect(industry.id) { viewModel.ensureIndustry(industry) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = industry.label,
                    actionLabel = "More",
                    onAction = { onIndustryClick(industry) }
                )
                when (val s = industries[industry.id]) {
                    null, LoadState.Loading -> CarouselSkeleton(cardWidth = 120.dp)
                    is LoadState.Error -> ErrorState(s.error, onRetry = { viewModel.ensureIndustry(industry, force = true) })
                    is LoadState.Success ->
                        if (s.data.isEmpty()) {
                            Text(
                                "No performers found for this industry.",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else PersonCarousel(s.data, onPersonClick, cardWidth = 120.dp)
                }
            }
        }

        // Recently viewed
        if (recents.isNotEmpty()) {
            item(key = "recents") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Recently viewed")
                    PersonCarousel(recents, onPersonClick, cardWidth = 120.dp)
                }
            }
        }
    }
}

@Composable
private fun PopularGrid(
    people: List<PersonSummary>,
    loading: Boolean,
    onPersonClick: (PersonSummary) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PersonCardSkeleton(Modifier.weight(1f))
                    PersonCardSkeleton(Modifier.weight(1f))
                }
            }
        } else {
            people.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { p ->
                        PersonCard(p, onClick = { onPersonClick(p) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    state: LoadState<List<PersonSummary>>,
    onPersonClick: (PersonSummary) -> Unit,
    onRetry: () -> Unit
) {
    val hero = (state as? LoadState.Success)?.data?.firstOrNull()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (hero != null) {
            RemoteImage(
                path = hero.profilePath,
                kind = ImageKind.HERO,
                contentDescription = "Photo of ${hero.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Scrims: top for brand legibility, bottom for text
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(SGColors.DeepBlack.copy(alpha = 0.65f), Color.Transparent, Color.Transparent)
                    )
                )
        )
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).bottomScrim(1f))

        // Brand row
        Row(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(SGColors.AccentGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text("StarGallery", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            when {
                hero != null -> {
                    Text("TRENDING THIS WEEK", style = MaterialTheme.typography.labelMedium, color = SGColors.SubtleCyan)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hero.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        modifier = Modifier.semantics { contentDescription = "Featured performer ${hero.name}" }
                    )
                    hero.knownFor.firstOrNull()?.let {
                        Text("Known for $it", style = MaterialTheme.typography.bodyMedium, color = SGColors.CoolGrey)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { onPersonClick(hero) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("View profile") }
                }
                state is LoadState.Error -> {
                    Text("Discover performers worldwide", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(state.error.message(), style = MaterialTheme.typography.bodyMedium, color = SGColors.CoolGrey)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
                else -> {
                    Text("Discover performers worldwide", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                }
            }
        }
    }
}
