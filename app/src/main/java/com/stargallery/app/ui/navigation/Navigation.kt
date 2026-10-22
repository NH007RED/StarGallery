package com.stargallery.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stargallery.app.AppContainer
import com.stargallery.app.ui.screens.ExploreScreen
import com.stargallery.app.ui.screens.FavoritesScreen
import com.stargallery.app.ui.screens.HomeScreen
import com.stargallery.app.ui.screens.ProfileScreen
import com.stargallery.app.ui.screens.SettingsScreen
import com.stargallery.app.viewmodel.ExploreViewModel
import com.stargallery.app.viewmodel.FavoritesViewModel
import com.stargallery.app.viewmodel.HomeViewModel
import com.stargallery.app.viewmodel.ProfileViewModel
import com.stargallery.app.viewmodel.SettingsViewModel
import com.stargallery.app.viewmodel.viewModelFactory

object Routes {
    const val HOME = "home"
    const val EXPLORE_PATTERN = "explore?industry={industry}&query={query}"
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val PROFILE_PATTERN = "profile/{personId}"

    fun profile(id: Int) = "profile/$id"
    fun explore(industryId: String? = null, query: String? = null): String {
        val params = buildList {
            industryId?.let { add("industry=${Uri.encode(it)}") }
            query?.let { add("query=${Uri.encode(it)}") }
        }
        return if (params.isEmpty()) EXPLORE else "$EXPLORE?${params.joinToString("&")}"
    }
}

private data class Tab(
    val route: String,
    val pattern: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val tabs = listOf(
    Tab(Routes.HOME, Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.EXPLORE, Routes.EXPLORE_PATTERN, "Explore", Icons.Outlined.Explore, Icons.Filled.Explore),
    Tab(Routes.FAVORITES, Routes.FAVORITES, "Favorites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Tab(Routes.SETTINGS, Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.route == tab.pattern } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.pattern } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory { HomeViewModel(container.discovery, container.library) }
                )
                HomeScreen(
                    viewModel = vm,
                    onPersonClick = { navController.navigate(Routes.profile(it.id)) },
                    onSearch = { navController.navigate(Routes.explore(query = it)) },
                    onIndustryClick = { navController.navigate(Routes.explore(industryId = it.id)) },
                    onSeeAllPopular = { navController.navigateToTab(Routes.EXPLORE) }
                )
            }

            composable(
                route = Routes.EXPLORE_PATTERN,
                arguments = listOf(
                    navArgument("industry") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val industryId = entry.arguments?.getString("industry")
                val query = entry.arguments?.getString("query")
                val vm: ExploreViewModel = viewModel(
                    factory = viewModelFactory { ExploreViewModel(container.discovery, industryId) }
                )
                ExploreScreen(
                    viewModel = vm,
                    initialQuery = query,
                    initialIndustryId = industryId,
                    onPersonClick = { navController.navigate(Routes.profile(it.id)) }
                )
            }

            composable(Routes.FAVORITES) {
                val vm: FavoritesViewModel = viewModel(
                    factory = viewModelFactory { FavoritesViewModel(container.library) }
                )
                FavoritesScreen(
                    viewModel = vm,
                    onPersonClick = { navController.navigate(Routes.profile(it.id)) },
                    onExplore = { navController.navigateToTab(Routes.EXPLORE) }
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { SettingsViewModel(container.settingsStore, container.library) }
                )
                SettingsScreen(vm)
            }

            composable(
                route = Routes.PROFILE_PATTERN,
                arguments = listOf(navArgument("personId") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("personId") ?: return@composable
                val vm: ProfileViewModel = viewModel(
                    factory = viewModelFactory { ProfileViewModel(id, container.profiles, container.library) }
                )
                ProfileScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPersonClick = { navController.navigate(Routes.profile(it.id)) }
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
