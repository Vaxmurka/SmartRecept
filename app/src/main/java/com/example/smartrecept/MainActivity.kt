package com.example.smartrecept

import AddEditRecipeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.*
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.data.settings.UserPreferencesRepository
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import com.example.smartrecept.data.recipes.DatasourceRecipes
import com.example.smartrecept.ui.screens.CookingScreen

import com.example.smartrecept.ui.screens.SettingsScreen
import com.example.smartrecept.ui.screens.SearchScreen
import com.example.smartrecept.ui.screens.HomeScreen
import com.example.smartrecept.ui.screens.FavoritesScreen
import com.example.smartrecept.ui.screens.JournalScreen
import com.example.smartrecept.ui.screens.PreviewRecipeViewModel
import com.example.smartrecept.ui.screens.RecipeDetailScreen

import com.example.smartrecept.ui.theme.SmartReceptTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            DatasourceRecipes(applicationContext).initializeDatabase()
        }

        setContent {
            SmartReceptApp()
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Главная", Icons.Default.Home)
    object Search : Screen("search", "Поиск", Icons.Default.Search)
    object Favorites : Screen("favorites", "Избранное", Icons.Default.Favorite)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    object AddRecipe : Screen("addRecipe", "Новый рецепт", Icons.Default.Add)
    object Journal : Screen("journal", "Журнал", Icons.Default.Restaurant)
}

val filterChipsList = listOf("Завтрак", "Основное", "Суп", "Салат", "Гарнир", "Быстрое", "Сладкое", "Десерт")

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Journal,
    Screen.Settings
)

fun getSystemTheme(context: Context): String {
    return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> "dark"
        Configuration.UI_MODE_NIGHT_NO -> "light"
        else -> "system"
    }
}

@Composable
fun SmartReceptApp() {
    val context = LocalContext.current
    val userPrefsRepo = remember { UserPreferencesRepository(context) }

    val userPrefs by userPrefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())

//    MaterialTheme(
//        colorScheme = when (userPrefs.themeMode) {
//            "light" -> lightColorScheme()
//            "dark" -> darkColorScheme()
//            else -> if (getSystemTheme(LocalContext.current) === "light") lightColorScheme() else darkColorScheme()
//        }
//    )
    SmartReceptTheme(
        isDarken = when (userPrefs.themeMode) {
            "light" -> false
            "dark" -> true
            else -> if (getSystemTheme(LocalContext.current) === "light") false else true
        }
    )
    {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                AnimatedVisibility(
                    visible = currentDestination?.route in listOf("home", "search", "journal", "settings"),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
                ) {
                    NavigationBar(
                        modifier = Modifier.height(60.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                modifier = Modifier.padding(vertical = 8.dp),
                                icon = {
                                    val icon = if (currentDestination?.route == screen.route) {
                                        when (screen) {
                                            Screen.Home -> Icons.Filled.Home
                                            Screen.Search -> Icons.Filled.Search
                                            Screen.Favorites -> Icons.Filled.Favorite
                                            Screen.Journal -> Icons.Filled.Restaurant
                                            Screen.Settings -> Icons.Filled.Settings
                                            else -> screen.icon
                                        }
                                    } else {
                                        when (screen) {
                                            Screen.Home -> Icons.Outlined.Home
                                            Screen.Search -> Icons.Outlined.Search
                                            Screen.Favorites -> Icons.Outlined.Favorite
                                            Screen.Journal -> Icons.Outlined.Restaurant
                                            Screen.Settings -> Icons.Outlined.Settings
                                            else -> screen.icon
                                        }
                                    }
                                    Icon(icon, contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp))
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        lineHeight = 16.sp
                                    )
                                },
                                selected = currentDestination?.route == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        }
                    }
                }

            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding).navigationBarsPadding()
            ) {
                animatedComposable(Screen.Home.route) {
                    HomeScreen(repository = userPrefsRepo, navController = navController)
                }
                animatedComposable(Screen.Search.route) {
                    SearchScreen(repository = userPrefsRepo, navController = navController)
                }
                animatedComposable(Screen.Favorites.route) {
                    FavoritesScreen(repository = userPrefsRepo, navController = navController)
                }
                animatedComposable(Screen.Journal.route) {
                    JournalScreen(repository = userPrefsRepo, navController = navController)
                }
                animatedComposable(Screen.Settings.route) {
                    SettingsScreen(repository = userPrefsRepo)
                }
                animatedComposable(
                    "recipe/{id}",
                    enterDirection = AnimatedContentTransitionScope.SlideDirection.Up,
                    exitDirection = AnimatedContentTransitionScope.SlideDirection.Down,
                    popEnterDirection = AnimatedContentTransitionScope.SlideDirection.Down,
                    popExitDirection = AnimatedContentTransitionScope.SlideDirection.Up,
                    duration = 400
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                    if (recipeId != null) {
                        RecipeDetailScreen(
                            recipeId = recipeId,
                            repository = userPrefsRepo,
                            navController = navController
                        )
                    }
                }
                animatedComposable(
                    "recipe/cook/{id}",
                    enterDirection = AnimatedContentTransitionScope.SlideDirection.Up,
                    exitDirection = AnimatedContentTransitionScope.SlideDirection.Down,
                    popEnterDirection = AnimatedContentTransitionScope.SlideDirection.Down,
                    popExitDirection = AnimatedContentTransitionScope.SlideDirection.Up,
                    duration = 400
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                    if (recipeId != null) {
                        CookingScreen(
                            recipeId = recipeId,
                            repository = userPrefsRepo,
                            navController = navController
                        )
                    }
                }
                animatedComposable(
                    "addEditRecipe/{id}",
                    enterDirection = AnimatedContentTransitionScope.SlideDirection.Up
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                    if (recipeId != null) {
                        AddEditRecipeScreen(
                            recipeId = if(recipeId != 0) recipeId else null,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.animatedComposable(
    route: String,
    enterDirection: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
    exitDirection: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
    popEnterDirection: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
    popExitDirection: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
    duration: Int = 300,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideIntoContainer(enterDirection, tween(duration)) + fadeIn(tween(duration))
        },
        exitTransition = {
            slideOutOfContainer(exitDirection, tween(duration)) + fadeOut(tween(duration))
        },
        popEnterTransition = {
            slideIntoContainer(popEnterDirection, tween(duration)) + fadeIn(tween(duration))
        },
        popExitTransition = {
            slideOutOfContainer(popExitDirection, tween(duration)) + fadeOut(tween(duration))
        }
    ) { backStackEntry ->
        content(backStackEntry)
    }
}

@Preview(showBackground = true, name = "Home Screen", device = "spec:width=411dp,height=891dp")
@Composable
fun HomeScreenPreview() {
    SmartReceptTheme {
        val navController = rememberNavController()
        val mockRepo = UserPreferencesRepository(LocalContext.current)
        val previewViewModel = PreviewRecipeViewModel()

        HomeScreen(
            repository = mockRepo,
            navController = navController,
            previewViewModel = previewViewModel
        )
    }
}