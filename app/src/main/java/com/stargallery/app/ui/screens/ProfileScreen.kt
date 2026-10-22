package com.stargallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.domain.FilmCredit
import com.stargallery.app.domain.PersonDetails
import com.stargallery.app.domain.PersonProfile
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.ui.components.ErrorState
import com.stargallery.app.ui.components.PersonCarousel
import com.stargallery.app.ui.components.RemoteImage
import com.stargallery.app.ui.components.SectionHeader
import com.stargallery.app.ui.components.shimmer
import com.stargallery.app.ui.theme.SGColors
import com.stargallery.app.util.ImageKind
import com.stargallery.app.util.ageFrom
import com.stargallery.app.util.formatIsoDate
import com.stargallery.app.viewmodel.ProfileViewModel
import java.text.DateFormat
import java.util.Date

private val HeaderHeight = 440.dp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onPersonClick: (PersonSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val profile = ui.profile

    Box(modifier.fillMaxSize()) {
        when {
            profile != null -> ProfileContent(
                profile = profile,
                related = ui.related,
                isFavorite = isFavorite,
                onBack = onBack,
                onToggleFavorite = viewModel::toggleFavorite,
                onRefresh = viewModel::retry,
                onPersonClick = onPersonClick
            )
            ui.error != null -> {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    BackButton(onBack, Modifier.padding(8.dp))
                    ErrorState(ui.error!!, onRetry = viewModel::retry, modifier = Modifier.weight(1f))
                }
            }
            else -> ProfileSkeleton(onBack)
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
private fun ProfileSkeleton(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(HeaderHeight).shimmer()) {
            BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        repeat(4) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().height(18.dp).clip(RoundedCornerShape(6.dp)).shimmer())
        }
    }
}

@Composable
private fun ProfileContent(
    profile: PersonProfile,
    related: List<PersonSummary>,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onPersonClick: (PersonSummary) -> Unit
) {
    val d = profile.details
    val listState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val headerPx = with(density) { HeaderHeight.toPx() }
    // Collapsing toolbar: 0 = fully expanded, 1 = collapsed.
    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / (headerPx * 0.6f)).coerceIn(0f, 1f)
        }
    }
    val uriHandler = LocalUriHandler.current

    val byYear: List<Pair<Int?, List<FilmCredit>>> = remember(profile.filmography) {
        profile.filmography
            .groupBy { it.year }
            .toList()
            .sortedWith(compareByDescending<Pair<Int?, List<FilmCredit>>> { it.first != null }.thenByDescending { it.first ?: 0 })
            .map { (year, list) -> year to list.sortedByDescending { it.releaseDate.orEmpty() } }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header") { ProfileHeader(d, listState.firstVisibleItemScrollOffset.takeIf { listState.firstVisibleItemIndex == 0 } ?: 0) }

            if (profile.fromCache) {
                item(key = "offline") {
                    val whenText = profile.cachedAt?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
                    InfoBanner(
                        "Showing a saved copy" + (whenText?.let { " from $it" } ?: "") + ". Tap refresh to update when online.",
                        actionLabel = "Refresh",
                        onAction = onRefresh
                    )
                }
            }
            if (profile.partial) {
                item(key = "partial") {
                    InfoBanner("Some details (filmography or photos) couldn't be loaded.", "Retry", onRefresh)
                }
            }

            item(key = "facts") { FactsCard(d) }

            item(key = "bio") { BiographySection(d) }

            if (profile.images.size > 1) {
                item(key = "photos") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Photos")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(profile.images.take(20), key = { it.filePath }) { img ->
                                RemoteImage(
                                    path = img.filePath,
                                    kind = ImageKind.PROFILE,
                                    contentDescription = "Photo of ${d.name}",
                                    modifier = Modifier.width(110.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }

            if (profile.knownForMovies.isNotEmpty()) {
                item(key = "known") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Known for", subtitle = "Most popular credits on TMDB")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(profile.knownForMovies, key = { it.movieId }) { film -> KnownForCard(film) }
                        }
                    }
                }
            }

            if (related.isNotEmpty()) {
                item(key = "related") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Related performers", subtitle = "Billed alongside ${d.name} in popular films")
                        PersonCarousel(related, onPersonClick, cardWidth = 120.dp)
                    }
                }
            }

            item(key = "film-header") {
                SectionHeader(
                    "Filmography",
                    subtitle = if (profile.filmography.isEmpty()) null else "${profile.filmography.size} acting credits, newest first"
                )
            }
            if (profile.filmography.isEmpty()) {
                item(key = "film-empty") {
                    Text(
                        "No acting credits are listed on TMDB for this person.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            byYear.forEach { (year, credits) ->
                item(key = "year-${year ?: "unknown"}") {
                    Text(
                        year?.toString() ?: "Release date unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                items(credits) { film ->
                    FilmRow(film)
                }
            }

            item(key = "links") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row {
                        TextButton(onClick = { uriHandler.openUri("https://www.themoviedb.org/person/${d.id}") }) {
                            Text("View on TMDB")
                        }
                        d.imdbId?.let { imdb ->
                            TextButton(onClick = { uriHandler.openUri("https://www.imdb.com/name/$imdb/") }) {
                                Text("View on IMDb")
                            }
                        }
                    }
                    Text(
                        "Data provided by TMDB. StarGallery is not endorsed or certified by TMDB.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Collapsing toolbar overlay
        val barColor = MaterialTheme.colorScheme.surface.copy(alpha = collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack)
            Text(
                d.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).graphicsLayer { alpha = collapse }
            )
            IconButton(
                onClick = onToggleFavorite,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    contentColor = if (isFavorite) SGColors.SoftMagenta else Color.White
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove ${d.name} from favourites" else "Add ${d.name} to favourites"
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(d: PersonDetails, scrollOffset: Int) {
    Box(modifier = Modifier.fillMaxWidth().height(HeaderHeight)) {
        // Large cinematic backdrop with parallax
        RemoteImage(
            path = d.profilePath,
            kind = ImageKind.HERO,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = scrollOffset * 0.5f }
        )
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        SGColors.DeepBlack.copy(alpha = 0.55f),
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            RemoteImage(
                path = d.profilePath,
                kind = ImageKind.PROFILE,
                contentDescription = "Portrait of ${d.name}",
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, SGColors.AccentGradient, RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(
                    d.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { contentDescription = "Name: ${d.name}" }
                )
                Text(
                    d.knownForDepartment ?: "Department not listed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SGColors.SubtleCyan
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun FactsCard(d: PersonDetails) {
    val unavailable = "Not available on TMDB"
    val born = formatIsoDate(d.birthday)
    val age = ageFrom(d.birthday, d.deathday)
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Fact("Born", born?.let { it + (age?.let { a -> if (d.deathday == null) " (age $a)" else "" } ?: "") } ?: unavailable)
            d.deathday?.let { Fact("Died", (formatIsoDate(it) ?: it) + (age?.let { a -> " (aged $a)" } ?: "")) }
            Fact("Place of birth", d.placeOfBirth ?: unavailable)
            Fact("Known for department", d.knownForDepartment ?: unavailable)
            if (d.alsoKnownAs.isNotEmpty()) Fact("Also known as", d.alsoKnownAs.take(4).joinToString(", "))
            Text(
                "Place of birth is free text entered by TMDB contributors. StarGallery does not infer nationality or country of origin.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BiographySection(d: PersonDetails) {
    var expanded by rememberSaveable(d.id) { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Biography", style = MaterialTheme.typography.titleLarge)
        val bio = d.biography
        if (bio == null) {
            Text(
                "No biography is available on TMDB in the selected language. You can change the content language in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            var overflows by remember(bio) { mutableStateOf(false) }
            Text(
                bio,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                modifier = Modifier.fillMaxWidth()
            )
            if (overflows || expanded) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Read more")
                }
            }
        }
    }
}

@Composable
private fun KnownForCard(film: FilmCredit) {
    Column(Modifier.width(120.dp)) {
        RemoteImage(
            path = film.posterPath,
            kind = ImageKind.POSTER,
            contentDescription = "Poster of ${film.title}",
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(film.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        film.year?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun FilmRow(film: FilmCredit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteImage(
            path = film.posterPath,
            kind = ImageKind.POSTER,
            contentDescription = "Poster of ${film.title}",
            modifier = Modifier.width(44.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(film.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                film.character?.let { "as $it" } ?: "Role not listed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        film.voteAverage?.takeIf { it > 0 }?.let {
            Text("★ %.1f".format(it), style = MaterialTheme.typography.labelLarge, color = SGColors.SubtleCyan)
        }
    }
}
