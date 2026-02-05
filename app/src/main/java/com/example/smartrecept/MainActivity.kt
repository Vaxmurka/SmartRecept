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
import android.os.Build
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import com.example.smartrecept.data.recipes.DatasourceRecipes
import com.example.smartrecept.data.settings.LocaleUtils
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rememberScrollHandler

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Создаём репозиторий НА БАЗОВОМ контексте
        val repo = UserPreferencesRepository(newBase)

        // Синхронно читаем язык (допустимо здесь)
        val language = runBlocking {
            repo.preferencesFlow
                .map { it.language }
                .first()
        }

        // Применяем локаль
        val localizedContext = LocaleUtils.setAppLocale(newBase, language)

        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Улучшаем производительность анимаций
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Оптимизация для анимаций
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Создаем репозиторий
        val userPrefsRepo = UserPreferencesRepository(this)

        // Запускаем инициализацию БД в фоне
        CoroutineScope(Dispatchers.IO).launch {
            DatasourceRecipes(applicationContext).initializeDatabase()
        }

        setContent {
            SmartReceptApp(userPrefsRepo = userPrefsRepo)
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
fun SmartReceptApp(userPrefsRepo: UserPreferencesRepository) {
    val context = LocalContext.current
    val scrollHandler = rememberScrollHandler()
    val userPrefs by userPrefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())

    SmartReceptTheme(
        isDarken = when (userPrefs.themeMode) {
            "light" -> false
            "dark" -> true
            else -> getSystemTheme(LocalContext.current) == "dark"
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
                        .padding(innerPadding)
                        .navigationBarsPadding()
                ) {
                    // Главные экраны с боковой анимацией
                    animatedComposable(
                        Screen.Home.route,
                        animationType = NavigationAnimation.HORIZONTAL
                    ) {
                        HomeScreen(
                            repository = userPrefsRepo,
                            navController = navController,
                            scrollHandler = scrollHandler
                        )
                    }

                    animatedComposable(
                        Screen.Search.route,
                        animationType = NavigationAnimation.HORIZONTAL
                    ) {
                        SearchScreen(
                            repository = userPrefsRepo,
                            navController = navController,
                            scrollHandler = scrollHandler
                        )
                    }

                    // Камера с плавным появлением
                    animatedComposable(
                        "camera/{purpose}",
                        animationType = NavigationAnimation.FADE
                    ) { backStackEntry ->
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

                    animatedComposable(
                        Screen.Favorites.route,
                        animationType = NavigationAnimation.HORIZONTAL
                    ) {
                        FavoritesScreen(repository = userPrefsRepo, navController = navController)
                    }

                    animatedComposable(
                        Screen.Journal.route,
                        animationType = NavigationAnimation.HORIZONTAL
                    ) {
                        JournalScreen(repository = userPrefsRepo, navController = navController)
                    }

                    animatedComposable(
                        Screen.Settings.route,
                        animationType = NavigationAnimation.HORIZONTAL
                    ) {
                        SettingsScreen(
                            repository = userPrefsRepo,
                            navController = navController,
                            scrollHandler = scrollHandler
                        )
                    }

                    // Детали рецепта - вертикальная анимация
                    animatedComposable(
                        "recipe/{id}",
                        animationType = NavigationAnimation.VERTICAL
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

                    // Экран приготовления - вертикальная анимация
                    animatedComposable(
                        "recipe/cook/{id}",
                        animationType = NavigationAnimation.VERTICAL
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

                    // Добавление/редактирование рецепта - вертикальная анимация
                    animatedComposable(
                        "addEditRecipe/{id}",
                        animationType = NavigationAnimation.VERTICAL
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                        if (recipeId != null) {
                            AddEditRecipeScreen(
                                recipeId = if (recipeId != 0) recipeId else null,
                                navController = navController
                            )
                        }
                    }

                    // AI ассистент - плавное появление
                    animatedComposable(
                        Screen.AIAssistant.route,
                        animationType = NavigationAnimation.FADE
                    ) {
                        AIassistLogic(navController = navController)
                    }

                    // Gemini тест - плавное появление
                    animatedComposable(
                        "gemini_test",
                        animationType = NavigationAnimation.FADE
                    ) {
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
                visible = currentRoute in bottomNavRoutes,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(350, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(250, easing = FastOutSlowInEasing)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CustomBottomNavigation(
                    navController = navController,
                    scrollHandler = scrollHandler
                )
            }
        }
    }
}

// Перечисление для типов анимации
enum class NavigationAnimation {
    HORIZONTAL,   // Горизонтальный слайд для основных экранов
    VERTICAL,     // Вертикальный слайд для деталей
    FADE,         // Плавное появление
    NONE          // Без анимации
}

// Улучшенная функция для анимированных переходов
fun NavGraphBuilder.animatedComposable(
    route: String,
    animationType: NavigationAnimation = NavigationAnimation.HORIZONTAL,
    duration: Int = 500,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            when (animationType) {
                NavigationAnimation.HORIZONTAL -> {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffset = { it }
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.VERTICAL -> {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffset = { -it }
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.FADE -> {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                NavigationAnimation.NONE -> {
                    fadeIn(animationSpec = tween(1))
                }
            }
        },
        exitTransition = {
            when (animationType) {
                NavigationAnimation.HORIZONTAL -> {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        targetOffset = { it }
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = duration - 50,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.VERTICAL -> {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(
                            durationMillis = duration - 50,
                            easing = FastOutSlowInEasing
                        ),
                        targetOffset = { it }
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = duration - 50,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.FADE -> {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                NavigationAnimation.NONE -> {
                    fadeOut(animationSpec = tween(1))
                }
            }
        },
        popEnterTransition = {
            when (animationType) {
                NavigationAnimation.HORIZONTAL -> {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffset = { -it }
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.VERTICAL -> {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(
                            durationMillis = duration + 50,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffset = { it }
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = duration + 50,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.FADE -> {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                NavigationAnimation.NONE -> {
                    fadeIn(animationSpec = tween(1))
                }
            }
        },
        popExitTransition = {
            when (animationType) {
                NavigationAnimation.HORIZONTAL -> {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(
                            durationMillis = duration - 50,
                            easing = FastOutSlowInEasing
                        ),
                        targetOffset = { -it }
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = duration - 50,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.VERTICAL -> {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        targetOffset = { -it }
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = LinearEasing
                        )
                    )
                }
                NavigationAnimation.FADE -> {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                NavigationAnimation.NONE -> {
                    fadeOut(animationSpec = tween(1))
                }
            }
        }
    ) { backStackEntry ->
        content(backStackEntry)
    }
}