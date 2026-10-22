package com.stargallery.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stargallery.app.ui.theme.SGColors
import com.stargallery.app.util.AppError
import kotlin.math.cos
import kotlin.math.sin

/** Original illustration: a portrait frame with a star, drawn with Canvas. */
@Composable
fun StarFrameIllustration(modifier: Modifier = Modifier.size(120.dp)) {
    Canvas(modifier = modifier.semantics { contentDescription = "Illustration of a star inside a picture frame" }) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.035f
        val frameTopLeft = Offset(w * 0.2f, h * 0.12f)
        val frameSize = Size(w * 0.6f, h * 0.76f)
        drawRoundRect(
            brush = SGColors.AccentGradient,
            topLeft = frameTopLeft,
            size = frameSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = Stroke(width = stroke)
        )
        // corner ticks
        val tick = w * 0.12f
        val c = SGColors.SubtleCyan
        fun line(a: Offset, b: Offset) = drawLine(c, a, b, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
        val l = w * 0.1f; val t = h * 0.06f; val r = w * 0.9f; val btm = h * 0.94f
        line(Offset(l, t + tick), Offset(l, t)); line(Offset(l, t), Offset(l + tick, t))
        line(Offset(r, t + tick), Offset(r, t)); line(Offset(r, t), Offset(r - tick, t))
        line(Offset(l, btm - tick), Offset(l, btm)); line(Offset(l, btm), Offset(l + tick, btm))
        line(Offset(r, btm - tick), Offset(r, btm)); line(Offset(r, btm), Offset(r - tick, btm))

        // five-point star
        val cx = w / 2f; val cy = h * 0.5f
        val outer = w * 0.17f; val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.PI / 5 * i - Math.PI / 2
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, brush = SGColors.AccentGradient)
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StarFrameIllustration()
        Spacer(Modifier.height(16.dp))
        Text(
            when (error) {
                AppError.Network -> "You're offline"
                AppError.Timeout -> "Taking too long"
                else -> "Couldn't load this"
            },
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            error.message(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StarFrameIllustration()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Footer for paginated lists: spinner while loading more, or an inline retry on failure. */
@Composable
fun LoadMoreFooter(isLoading: Boolean, error: AppError?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            error != null -> {
                Text(error.message(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
