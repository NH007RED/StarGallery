package com.stargallery.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.stargallery.app.data.local.ImageQuality
import com.stargallery.app.util.ImageKind
import com.stargallery.app.util.imageUrl

val LocalImageQuality = compositionLocalOf { ImageQuality.MEDIUM }

/** Animated shimmer used for skeleton loading and image placeholders. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.surfaceContainer
    val high = MaterialTheme.colorScheme.surfaceContainerHigh
    background(
        Brush.linearGradient(
            colors = listOf(base, high, base),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x + 400f, 300f)
        )
    )
}

/**
 * TMDB image with explicit loading (shimmer), error and "no image" states.
 * [contentDescription] must describe the image for accessibility.
 */
@Composable
fun RemoteImage(
    path: String?,
    kind: ImageKind,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val quality = LocalImageQuality.current
    val url = imageUrl(path, kind, quality)
    if (url == null) {
        ImagePlaceholder(modifier, noImage = true)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = { ImagePlaceholder(Modifier.fillMaxSize(), noImage = false) }
    )
}

@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, noImage: Boolean) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (noImage) Icons.Outlined.Person else Icons.Outlined.BrokenImage,
            contentDescription = if (noImage) "No photo available" else "Image failed to load",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Vertical scrim used over images so text stays readable. */
fun Modifier.bottomScrim(strength: Float = 0.9f): Modifier = background(
    Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color(0xFF05060F).copy(alpha = strength))
    )
)

fun Modifier.clipRounded(radius: Int): Modifier = clip(androidx.compose.foundation.shape.RoundedCornerShape(radius.dp))
