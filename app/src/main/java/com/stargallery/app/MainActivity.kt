package com.stargallery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stargallery.app.data.local.AppSettings
import com.stargallery.app.data.local.ThemeMode
import com.stargallery.app.data.remote.TmdbConfig
import com.stargallery.app.ui.components.LocalImageQuality
import com.stargallery.app.ui.navigation.AppNavigation
import com.stargallery.app.ui.screens.NotConfiguredScreen
import com.stargallery.app.ui.screens.SplashScreen
import com.stargallery.app.ui.theme.StarGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as StarGalleryApp).container

        setContent {
            val settings by container.settingsStore.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // rememberSaveable keeps the splash from replaying on rotation/config change.
            var splashDone by rememberSaveable { mutableStateOf(false) }

            StarGalleryTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalImageQuality provides settings.imageQuality) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        AnimatedContent(
                            targetState = splashDone,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "splash"
                        ) { done ->
                            when {
                                !done -> SplashScreen(onFinished = { splashDone = true })
                                !TmdbConfig.isConfigured -> NotConfiguredScreen()
                                else -> AppNavigation(container)
                            }
                        }
                    }
                }
            }
        }
    }
}
