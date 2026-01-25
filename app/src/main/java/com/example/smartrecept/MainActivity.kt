package com.example.smartrecept

import com.example.smartrecept.ui.screens.AddEditRecipeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.Alignment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import com.example.smartrecept.data.recipes.DatasourceRecipes
import com.example.smartrecept.ui.components.AIassistLogic
import com.example.smartrecept.ui.components.CameraScreen
import com.example.smartrecept.ui.components.CustomBottomNavigation
import com.example.smartrecept.ui.screens.CookingScreen

import com.example.smartrecept.ui.screens.SettingsScreen
import com.example.smartrecept.ui.screens.SearchScreen
import com.example.smartrecept.ui.screens.HomeScreen
import com.example.smartrecept.ui.screens.FavoritesScreen
import com.example.smartrecept.ui.screens.GeminiTestScreen
import com.example.smartrecept.ui.screens.JournalScreen
import com.example.smartrecept.ui.screens.RecipeDetailScreen

import com.example.smartrecept.ui.theme.SmartReceptTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rememberScrollHandler

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
    object AIAssistant : Screen("ai_assistant", "AI Ассистент", Icons.Default.Chat)
    object GeminiTest : Screen("gemini_test", "AI Тест", Icons.Default.SmartToy)
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
    val scrollHandler = rememberScrollHandler()
    val userPrefs by userPrefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())

    SmartReceptTheme(
        isDarken = when (userPrefs.themeMode) {
            "light" -> false
            "dark" -> true
            else -> if (getSystemTheme(LocalContext.current) === "light") false else true
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Scaffold { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .padding(innerPadding).navigationBarsPadding()
                ) {
                    animatedComposable(Screen.Home.route) {
                        HomeScreen(repository = userPrefsRepo, navController = navController, scrollHandler = scrollHandler)
                    }
                    animatedComposable(Screen.Search.route) {
                        SearchScreen(repository = userPrefsRepo, navController = navController, scrollHandler = scrollHandler)
                    }
                    // В NavGraph
                    animatedComposable("camera/{purpose}") { backStackEntry ->
                        val purpose = backStackEntry.arguments?.getString("purpose") ?: "main"
                        CameraScreen(
                            purpose = purpose,
                            onImageCaptured = { uri, purpose ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("camera_result", uri.toString())
                                navController.previousBackStackEntry?.savedStateHandle?.set("camera_purpose", purpose)
                                navController.popBackStack()
                            },
                            onError = {
                                navController.popBackStack()
                            }
                        )
                    }
                    animatedComposable(Screen.Favorites.route) {
                        FavoritesScreen(repository = userPrefsRepo, navController = navController)
                    }
                    animatedComposable(Screen.Journal.route) {
                        JournalScreen(repository = userPrefsRepo, navController = navController)
                    }
                    animatedComposable(Screen.Settings.route) {
                        SettingsScreen(repository = userPrefsRepo, scrollHandler = scrollHandler)
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

                    animatedComposable(Screen.AIAssistant.route) {
                        AIassistLogic(navController = navController)
                    }

                    animatedComposable("gemini_test") {
                        GeminiTestScreen(navController = navController)
                    }
                }
            }

            val bottomNavRoutes = listOf(
                Screen.Home.route,
                Screen.Search.route,
                Screen.Journal.route,
                Screen.Settings.route
            )

            AnimatedVisibility(
                // Панель видна, если текущий маршрут находится в нашем списке
                visible = currentRoute in bottomNavRoutes,
                // Добавляем красивую анимацию появления/исчезновения
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                // Выравниваем по низу, как и раньше
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Ваш компонент без изменений
                CustomBottomNavigation(navController = navController, scrollHandler = scrollHandler)
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