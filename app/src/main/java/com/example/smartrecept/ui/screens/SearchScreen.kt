// ui/screens/SearchScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.filterChipsList
import com.example.smartrecept.ui.components.CustomSearchPanel
import com.example.smartrecept.ui.components.RecipeCard
import com.example.smartrecept.ui.components.getTagColor


@Composable
fun SearchScreen(
    repository: UserPreferencesRepository,
    navController: NavHostController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())

    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        SearchScreenContent(
            recipes = recipes,
            onUpdateFavorite = { id, isFavorite ->
                viewModel.toggleFavorite(id, isFavorite)
            },
            navController = navController
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreenContent(
    recipes: List<Recipe>,
    onUpdateFavorite: (Int, Boolean) -> Unit,
    navController: NavHostController,
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

    Surface(Modifier.background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth()) {
            CustomSearchPanel(
                query = query,
                onQueryChange = { query = it },
                navController = navController,
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )
            FilterChips(selectedFilter, onSelect = { selectedFilter = it })
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.padding(16.dp)) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isFavorite = recipe.isFavorite,
                        onToggleFavorite = { onUpdateFavorite(recipe.id, !recipe.isFavorite) },
                        navController = navController,
                        onDelete = { viewModel.deleteRecipe(recipe.id) },
                        onEdit = { navController.navigate("addEditRecipe/${recipe.id}") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchViewWithFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {}
        FilterChips(selectedFilter, onFilterChange)
    }
}

@Composable
fun FilterChips(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val options = filterChipsList
    Row(
        Modifier
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
//            .padding(horizontal = 16.dp)
    ) {
        options.forEach { label ->
            val isSelected = label == selected
            AssistChip(
                onClick = {
                    onSelect(if (isSelected) null else label)
                },
                label = { Text(label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) getTagColor(label) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(end = 8.dp),
                border = BorderStroke(
                    color = Color.Transparent,
                    width = 1.dp,
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SearchPreview() {
    MaterialTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val userPrefsRepo = remember { UserPreferencesRepository(context) }

        SearchScreen(repository = userPrefsRepo, navController = navController)
    }
}