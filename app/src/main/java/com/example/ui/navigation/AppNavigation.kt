package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.repository.NewsRepository
import com.example.ui.screens.create.CreateScreen
import com.example.ui.screens.detail.DetailScreen
import com.example.ui.screens.detail.DetailViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.saved.SavedScreen
import com.example.ui.screens.saved.SavedViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.ThemeViewModel

data class BottomNavItem(val name: String, val route: Any, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

val bottomNavItems = listOf(
    BottomNavItem("Home", HomeRoute, Icons.Default.Home, "Trang chủ"),
    BottomNavItem("Search", SearchRoute, Icons.Default.Search, "Tìm kiếm"),
    BottomNavItem("Settings", SettingsRoute, Icons.Default.Settings, "Cài đặt")
)

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { NewsRepository(context) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isBottomBarVisible = currentDestination?.route?.let { route ->
                bottomNavItems.any { item -> route.contains(item.route::class.simpleName ?: "") }
            } == true
            
            if (isBottomBarVisible) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route?.contains(item.route::class.simpleName ?: "") == true } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = HomeRoute, modifier = Modifier.padding(innerPadding)) {
            composable<HomeRoute> {
                val viewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(repository) as T
                        }
                    }
                )
                HomeScreen(
                    viewModel = viewModel,
                    navigateToDetail = { id -> navController.navigate(DetailRoute(id)) },
                    navigateToSaved = { navController.navigate(SavedRoute) }
                )
            }
            
            composable<SearchRoute> {
                val viewModel: com.example.ui.screens.search.SearchViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.ui.screens.search.SearchViewModel(repository) as T
                        }
                    }
                )
                SearchScreen(
                    viewModel = viewModel,
                    navigateToDetail = { id -> navController.navigate(DetailRoute(id)) }
                )
            }
            

            composable<SettingsRoute> {
                val isDarkMode by themeViewModel.isDarkMode.collectAsState()
                
                val viewModel: com.example.ui.screens.settings.SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.ui.screens.settings.SettingsViewModel(repository) as T
                        }
                    }
                )
                SettingsScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { themeViewModel.toggleDarkMode(it) }
                )
            }
            
            composable<DetailRoute> { backStackEntry ->
                val detailRoute = backStackEntry.toRoute<DetailRoute>()
                
                val viewModel: DetailViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return DetailViewModel(repository, detailRoute.articleId) as T
                        }
                    }
                )
                DetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable<SavedRoute> {
                val viewModel: SavedViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SavedViewModel(repository) as T
                        }
                    }
                )
                SavedScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    navigateToDetail = { id -> navController.navigate(DetailRoute(id)) }
                )
            }
        }
    }
}

