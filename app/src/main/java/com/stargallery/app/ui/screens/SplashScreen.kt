package com.stargallery.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.stargallery.app.ui.components.StarFrameIllustration
import com.stargallery.app.ui.theme.SGColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Animated branded splash. Calls [onFinished] after the animation completes. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch {
                kotlinx.coroutines.delay(500)
                titleAlpha.animateTo(1f, tween(700))
            }
        }
        kotlinx.coroutines.delay(500)
        onFinished()
    }

    val glow = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SGColors.DeepBlack, SGColors.MidnightNavy, SGColors.DeepBlack))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(280.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(listOf(SGColors.ElectricViolet.copy(alpha = 0.55f), Color.Transparent)),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StarFrameIllustration(
                Modifier
                    .size(132.dp)
                    .graphicsLayer { alpha = logoAlpha.value }
                    .scale(logoScale.value)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "StarGallery",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.alpha(titleAlpha.value)
            )
            Text(
                "Global Actress Explorer",
                style = MaterialTheme.typography.labelLarge,
                color = SGColors.SubtleCyan,
                modifier = Modifier.alpha(titleAlpha.value)
            )
        }
    }
}
