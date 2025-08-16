// ui/screens/HomeScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.CustomSearchPanel
import com.example.smartrecept.ui.components.RecipeCard
import com.example.smartrecept.ui.components.RecipeDayCard
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun HomeScreen(
    repository: UserPreferencesRepository,
    navController: NavHostController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
    previewViewModel: PreviewRecipeViewModel? = null
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
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("addEditRecipe/${0}") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить рецепт")
                }
            }
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

    val filteredRecipes = recipes
        .filter {
            val matchesQuery = query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }

            val matchesFilter = selectedFilter == null ||
                    it.tags.any { tag -> tag.equals(selectedFilter, ignoreCase = true) }

            matchesQuery && matchesFilter
        }

    LazyColumn(Modifier.background(color = MaterialTheme.colorScheme.background)) {
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                CustomSearchPanel(
                    query = query,
                    onQueryChange = { query = it },
                    navController = navController,
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it }
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
//                    index == filteredRecipes.lastIndex ->
//                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
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
        if (recipeDay == null) {
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