package com.stargallery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.BuildConfig
import com.stargallery.app.data.local.ImageQuality
import com.stargallery.app.data.local.ThemeMode
import com.stargallery.app.data.remote.TmdbConfig
import com.stargallery.app.domain.ContentLanguages
import com.stargallery.app.util.formatBytes
import com.stargallery.app.viewmodel.SettingsViewModel

/** TODO: replace these with your own hosted pages before publishing. */
private const val PRIVACY_URL = "https://example.com/stargallery/privacy"
private const val SUPPORT_URL = "mailto:support@example.com?subject=StarGallery%20support"

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cachedProfiles by viewModel.cachedProfiles.collectAsStateWithLifecycle()
    val imageCacheBytes by viewModel.imageCacheBytes.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var picker by remember { mutableStateOf<Picker?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp))

            Group("Appearance") {
                Item("Theme", when (settings.themeMode) {
                    ThemeMode.SYSTEM -> "Follow system"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }) { picker = Picker.THEME }
                Item("Image quality", settings.imageQuality.label) { picker = Picker.QUALITY }
            }

            Group("Content") {
                Item(
                    "Content language",
                    ContentLanguages.all.firstOrNull { it.code == settings.language }?.label ?: settings.language,
                    supporting = "Language requested from TMDB for biographies and titles. The app interface itself is English."
                ) { picker = Picker.LANGUAGE }
            }

            Group("Storage & cache") {
                Item("Offline profiles", "$cachedProfiles saved", supporting = "Profiles you opened are kept for offline reading.") {}
                Item("Offline profile freshness", ttlLabel(settings.cacheTtlHours)) { picker = Picker.TTL }
                Item("Image & network cache", formatBytes(imageCacheBytes), supporting = "Tap to clear downloaded images.") {
                    confirm = Confirm("Clear image cache?", "Downloaded images will be fetched again when needed.") { viewModel.clearImageCache() }
                }
                Item("Clear offline profiles", "Favourites are kept") {
                    confirm = Confirm("Clear offline profiles?", "Saved profile details will be removed. Your favourites stay.") { viewModel.clearProfileCache() }
                }
                Item("Clear recently viewed", "") {
                    confirm = Confirm("Clear recently viewed?", "Your viewing history will be removed.") { viewModel.clearRecents() }
                }
            }

            Group("About") {
                Item("StarGallery", "Version ${BuildConfig.VERSION_NAME}") {}
                Item(
                    "Data source",
                    if (TmdbConfig.usesProxy) "TMDB via your backend proxy" else "TMDB API"
                ) {}
                Item("Privacy policy", "Opens in your browser") { uriHandler.openUri(PRIVACY_URL) }
                Item("Support", "Contact us") { uriHandler.openUri(SUPPORT_URL) }
            }

            Group("Legal") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TMDB attribution", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This product uses the TMDB API but is not endorsed or certified by TMDB. " +
                            "All performer, film and image data is provided by TMDB and remains the property of its respective owners.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { uriHandler.openUri("https://www.themoviedb.org/") }) { Text("themoviedb.org") }
                    Text(
                        "Place of birth and other biographical fields are contributor-entered text on TMDB and may be incomplete or inaccurate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }

    when (picker) {
        Picker.THEME -> OptionDialog(
            title = "Theme",
            options = listOf(ThemeMode.SYSTEM to "Follow system", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
            selected = settings.themeMode,
            onSelect = { viewModel.setTheme(it); picker = null },
            onDismiss = { picker = null }
        )
        Picker.QUALITY -> OptionDialog(
            title = "Image quality",
            options = ImageQuality.entries.map { it to it.label },
            selected = settings.imageQuality,
            onSelect = { viewModel.setImageQuality(it); picker = null },
            onDismiss = { picker = null }
        )
        Picker.LANGUAGE -> OptionDialog(
            title = "Content language",
            options = ContentLanguages.all.map { it.code to it.label },
            selected = settings.language,
            onSelect = { viewModel.setLanguage(it); picker = null },
            onDismiss = { picker = null }
        )
        Picker.TTL -> OptionDialog(
            title = "Keep offline profiles fresh for",
            options = listOf(24 to ttlLabel(24), 72 to ttlLabel(72), 168 to ttlLabel(168), 720 to ttlLabel(720)),
            selected = settings.cacheTtlHours,
            onSelect = { viewModel.setCacheTtlHours(it); picker = null },
            onDismiss = { picker = null }
        )
        null -> Unit
    }

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(c.title) },
            text = { Text(c.body) },
            confirmButton = { TextButton(onClick = { c.action(); confirm = null }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}

private enum class Picker { THEME, QUALITY, LANGUAGE, TTL }

private class Confirm(val title: String, val body: String, val action: () -> Unit)

private fun ttlLabel(hours: Int) = when (hours) {
    24 -> "1 day"
    72 -> "3 days"
    168 -> "7 days"
    720 -> "30 days"
    else -> "$hours hours"
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) { Column { content() } }
    }
}

@Composable
private fun Item(title: String, value: String, supporting: String? = null, onClick: () -> Unit) {
    Column {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (value.isNotEmpty()) {
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
