// ui/screens/HomeScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import ScrollHandler
import android.app.Application
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartrecept.data.settings.UserPreferencesRepository
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.CustomSearchPanel
import com.example.smartrecept.ui.components.RecipeCard
import com.example.smartrecept.ui.components.RecipeDayCard
import com.example.smartrecept.ui.components.navigateSingleTopTo
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun HomeScreen(
    repository: UserPreferencesRepository,
    navController: NavHostController,
    scrollHandler: ScrollHandler,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate("addEditRecipe/${0}") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (20).dp)
                            .size(width = 60.dp, height = 80.dp)
                            .graphicsLayer {
                                shadowElevation = 10.dp.toPx()
                                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                                clip = true
                            }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить рецепт",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    viewModel.refreshRecipe(recipes)
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .nestedScroll(scrollHandler.createNestedScrollConnection())
                        .graphicsLayer {
                            clip = true
                            shape = RectangleShape
                        }
                ) {
                    HomeScreenContent(
                        recipes = recipes,
                        navController = navController,
                        onUpdateFavorite = { id, isFavorite ->
                            viewModel.toggleFavorite(id, isFavorite)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    recipes: List<Recipe>,
    navController: NavHostController,
    onUpdateFavorite: (Int, Boolean) -> Unit,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    // Добавляем обработчик для клика по поиску
    val onClickSearch = {
        Log.d("HomeScreen", "Search clicked, navigating...")
        println("Search clicked, navigating...")
        navController.navigate("search") {
            launchSingleTop = true
            restoreState = true
        }
    }

    val searchTerms = query
        .split(" ")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val filteredRecipes = recipes.filter { recipe ->
        // Проверка на совпадение хотя бы по одному слову
        val matchesQuery = searchTerms.isEmpty() || searchTerms.all { term -> // all -> И, any -> ИЛИ
            recipe.title.contains(term, ignoreCase = true) ||
                    recipe.tags.any { tag -> tag.contains(term, ignoreCase = true) } ||
                    recipe.ingredients.any { ingredient -> ingredient.contains(term, ignoreCase = true) } ||
                    recipe.steps.any { step -> step.contains(term, ignoreCase = true) }
        }

        val matchesFilter = selectedFilter == null ||
                recipe.tags.any { tag -> tag.equals(selectedFilter, ignoreCase = true) }

        matchesQuery && matchesFilter
    }

    LazyColumn(Modifier.background(color = MaterialTheme.colorScheme.background)) {
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                CustomSearchPanel(
                    query = query,
                    readOnly = true,
                    onQueryChange = { query = it },
                    navController = navController,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onClickSearch = onClickSearch
                )
            }
        }

        item {
            CustomCard(
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                boxPadding = PaddingValues(bottom = 12.dp, start = 8.dp, end = 8.dp),
                outPadding = PaddingValues(bottom = 12.dp)
            ) {
                RecipeInDay(
                    recipes = recipes,
                    selectedFilter = selectedFilter,
                    navController = navController,
                    onFilterChange = { selectedFilter = it }
                )
            }
        }

        itemsIndexed(filteredRecipes) { index, recipe ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = when {
                    index == 0 && filteredRecipes.size == 1 ->
                        RoundedCornerShape(16.dp)
                    index == 0 ->
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    else ->
                        RectangleShape
                }
            ) {
                RecipeCard(
                    recipe = recipe,
                    isFavorite = recipe.isFavorite,
                    onToggleFavorite = { onUpdateFavorite(recipe.id, !recipe.isFavorite) },
                    navController = navController,
                    onHomePage = true,
                    onDelete = { viewModel.deleteRecipe(recipe.id) },
                    onEdit = { navController.navigate("addEditRecipe/${recipe.id}") }
                )
            }

            if (index < filteredRecipes.lastIndex) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun RecipeInDay(
    viewModel: RecipeViewModel = viewModel(),
    navController: NavHostController,
    recipes: List<Recipe>,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    val recipeDay by viewModel.recipeOfTheDay
    val isRefreshing by viewModel.isRefreshing

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    // При первом запуске
    LaunchedEffect(recipes) {
        if (recipes.isEmpty()) {
            // Если рецептов нет, очищаем рецепт дня
            viewModel.updateRecipeOfTheDay(emptyList())
        } else if (recipeDay == null || !recipes.any { it.id == recipeDay?.id }) {
            // Если рецепт дня null или был удален, выбираем новый
            viewModel.updateRecipeOfTheDay(recipes)
        }
    }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            viewModel.refreshRecipe(recipes)
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            FilterChips(selectedFilter, onFilterChange)
            recipeDay?.let { recipe ->
                RecipeDayCard(recipe = recipe, navController = navController)
            } ?: Text("Нет доступных рецептов", modifier = Modifier.padding(16.dp))
        }

    }
}