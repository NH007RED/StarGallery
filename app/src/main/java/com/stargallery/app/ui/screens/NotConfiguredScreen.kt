package com.stargallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stargallery.app.ui.components.StarFrameIllustration

/** Shown instead of the app until TmdbConfig contains credentials (or a proxy URL). */
@Composable
fun NotConfiguredScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        StarFrameIllustration(Modifier.size(128.dp))
        Spacer(Modifier.height(16.dp))
        Text("StarGallery", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "TMDB API isn't configured yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "StarGallery loads performer data from The Movie Database (TMDB). " +
                "This build was shipped without credentials, so there is nothing to show yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Set it up in 3 steps", style = MaterialTheme.typography.titleMedium)
                Step(1, "Create a free TMDB account and request an API key at themoviedb.org → Settings → API.")
                Step(2, "Open data/remote/TmdbConfig.kt in Android Studio and paste your API Read Access Token (or v3 API key) between the quotes.")
                Step(3, "Rebuild and run the app. This screen disappears automatically.")
                Text(
                    "app/src/main/java/com/stargallery/app/data/remote/TmdbConfig.kt",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { uriHandler.openUri("https://www.themoviedb.org/settings/api") }) {
            Text("Open TMDB API settings")
        }
        TextButton(onClick = { uriHandler.openUri("https://developer.themoviedb.org/docs/getting-started") }) {
            Text("TMDB developer docs")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "For a published app, use a backend proxy (TmdbConfig.BACKEND_PROXY_URL) instead of shipping a key inside the APK. See the README.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
