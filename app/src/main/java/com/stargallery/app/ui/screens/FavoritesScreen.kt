package com.stargallery.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.domain.PersonSummary
import com.stargallery.app.ui.components.EmptyState
import com.stargallery.app.ui.components.PersonCard
import com.stargallery.app.ui.theme.SGColors
import com.stargallery.app.viewmodel.FavoritesViewModel
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onPersonClick: (PersonSummary) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "Favourites",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp)
            )
            if (ui.all.isNotEmpty()) {
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Search saved profiles") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (ui.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search text")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            when {
                ui.all.isEmpty() -> EmptyState(
                    title = "No favourites yet",
                    message = "Tap the heart on a profile to save it here. Favourites are stored on this device and work offline.",
                    actionLabel = "Explore performers",
                    onAction = onExplore,
                    modifier = Modifier.weight(1f)
                )
                ui.visible.isEmpty() -> EmptyState(
                    title = "No matches",
                    message = "None of your saved profiles match \"${ui.query.trim()}\".",
                    actionLabel = "Clear search",
                    onAction = { viewModel.setQuery("") },
                    modifier = Modifier.weight(1f)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.visible, key = { it.id }) { person ->
                        Box {
                            PersonCard(person, onClick = { onPersonClick(person) })
                            IconButton(
                                onClick = {
                                    viewModel.remove(person.id)
                                    scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        val result = snackbar.showSnackbar(
                                            message = "${person.name} removed",
                                            actionLabel = "Undo",
                                            withDismissAction = true
                                        )
                                        if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(person)
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.45f),
                                    contentColor = SGColors.SoftMagenta
                                )
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Remove ${person.name} from favourites")
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}
