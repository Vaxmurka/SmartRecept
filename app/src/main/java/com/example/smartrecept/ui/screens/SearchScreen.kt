package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import ScrollHandler
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.filterChipsList
import com.example.smartrecept.ui.components.RecipeCard
import com.example.smartrecept.ui.components.getTagColor
import com.example.smartrecept.Screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.CustomSearchPanel
import kotlin.collections.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    repository: UserPreferencesRepository,
    navController: NavHostController,
    scrollHandler: ScrollHandler,
    viewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var useAndLogic by remember { mutableStateOf(false) }
    var showExtraFilters by remember { mutableStateOf(false) }

    // дополнительные фильтры
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlyCooked by remember { mutableStateOf(false) }
    var maxTime by remember { mutableStateOf<Int?>(null) } // в минутах

    val searchTerms = query
        .split(" ")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val recipes = viewModel.recipes.collectAsState().value

    val filteredRecipes = recipes.filter { recipe ->
        val matchesQuery = if (searchTerms.isEmpty()) {
            true
        } else if (useAndLogic) {
            searchTerms.all { term ->
                recipe.title.contains(term, ignoreCase = true) ||
                        recipe.tags.any { tag -> tag.contains(term, ignoreCase = true) } ||
                        recipe.ingredients.any { ing -> ing.contains(term, ignoreCase = true) } ||
                        recipe.steps.any { step -> step.contains(term, ignoreCase = true) }
            }
        } else {
            searchTerms.any { term ->
                recipe.title.contains(term, ignoreCase = true) ||
                        recipe.tags.any { tag -> tag.contains(term, ignoreCase = true) } ||
                        recipe.ingredients.any { ing -> ing.contains(term, ignoreCase = true) } ||
                        recipe.steps.any { step -> step.contains(term, ignoreCase = true) }
            }
        }

        val matchesTagFilter = selectedFilter == null ||
                recipe.tags.any { tag -> tag.equals(selectedFilter, ignoreCase = true) }

        val matchesFavorites = !onlyFavorites || recipe.isFavorite
        val matchesCooked = !onlyCooked || recipe.isCooked
        val matchesTime = maxTime == null || recipe.time <= maxTime!!.toString()

        matchesQuery && matchesTagFilter && matchesFavorites && matchesCooked && matchesTime
    }

    Scaffold(
        topBar = {
            CustomSearchPanel(
                query = query,
                onQueryChange = { query = it },
                navController = navController,
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 🔹 Кнопка для показа/скрытия фильтров
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showExtraFilters = !showExtraFilters }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Фильтры"
                    )
                }
            }

            // 🔹 Доп. фильтры (показываются по кнопке)
            if (showExtraFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = onlyFavorites,
                            onClick = { onlyFavorites = !onlyFavorites },
                            label = { Text("Любимое") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = onlyCooked,
                            onClick = { onlyCooked = !onlyCooked },
                            label = { Text("Готовил") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Время приготовления:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        AssistChip(
                            onClick = {
                                maxTime = if (maxTime == 40) null else 40
                            },
                            label = { Text("До 40 мин") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (maxTime == 40)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // 🔹 Переключатель логики поиска И/ИЛИ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Режим поиска:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = !useAndLogic,
                    onClick = { useAndLogic = false },
                    label = { Text("ИЛИ") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = useAndLogic,
                    onClick = { useAndLogic = true },
                    label = { Text("И") }
                )
            }

            // 🔹 Фильтры по тегам
            FilterChips(selected = selectedFilter, onSelect = { selectedFilter = it })
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            when {
                query.isBlank() -> {
                    // стартовый экран поиска
                    StartSearchContent(
                        suggestions = listOf("Паста", "Суп", "Курица", "Салат"),
                        onSuggestionClick = { query = it }
                    )
                    PopularRecipesSection(
                        recipes = recipes.take(3), // можно потом сделать "топ по лайкам"
                        navController = navController,
                        viewModel = viewModel
                    )
                }
                filteredRecipes.isNotEmpty() -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "По результатам поиска найдено: ",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))

                    val listState = rememberLazyListState()

                    LaunchedEffect(listState.isScrollInProgress) {
                        if (listState.isScrollInProgress) {
                            scrollHandler.handleLazyListScroll(listState)
                        }
                    }

                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        scrollHandler.handleLazyListScroll(listState)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
                        state = listState
                    ) {
                        itemsIndexed(filteredRecipes) { index, recipe ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shape = when {
                                    index == 0 && filteredRecipes.size == 1 ->
                                        RoundedCornerShape(16.dp)
                                    index == 0 ->
                                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    index == filteredRecipes.lastIndex ->
                                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    else ->
                                        RectangleShape
                                }
                            ) {
                                RecipeCard(
                                    recipe = recipe,
                                    isFavorite = recipe.isFavorite,
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(recipe.id, !recipe.isFavorite)
                                    },
                                    navController = navController,
                                    onDelete = { viewModel.deleteRecipe(recipe.id) },
                                    onEdit = { navController.navigate("addEditRecipe/${recipe.id}") },
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
                else -> {
                    // если ничего не найдено — возвращаемся к стартовому виду
                    StartSearchContent(
                        suggestions = listOf("Рыба", "Быстро", "Десерт", "Овощи"),
                        onSuggestionClick = { query = it }
                    )
                    PopularRecipesSection(
                        recipes = recipes.take(3),
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSearchContent(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Попробуйте поискать:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                AssistChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularRecipesSection(
    recipes: List<Recipe>,
    navController: NavHostController,
    viewModel: RecipeViewModel
) {
    CustomCard(
        shape = RoundedCornerShape(28.dp),
        boxPadding = PaddingValues(top = 16.dp, bottom = 4.dp),
        outPadding = PaddingValues(bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Популярное", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            recipes.forEachIndexed { index, recipe ->
                RecipeCard(
                    recipe = recipe,
                    isFavorite = recipe.isFavorite,
                    onToggleFavorite = {
                        viewModel.toggleFavorite(recipe.id, !recipe.isFavorite)
                    },
                    navController = navController,
                    onDelete = { viewModel.deleteRecipe(recipe.id) },
                    onEdit = { navController.navigate("addEditRecipe/${recipe.id}") }
                )

                if (index < recipes.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }

//        Text("Популярное", style = MaterialTheme.typography.titleMedium)
//        Spacer(Modifier.height(8.dp))
//        LazyColumn(modifier = Modifier) {
//            itemsIndexed(recipes) { index, recipe ->
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.surface,
//                    shape = when {
//                        index == 0 && recipes.size == 1 ->
//                            RoundedCornerShape(16.dp)
//                        index == 0 ->
//                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
//                        index == recipes.lastIndex ->
//                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
//                        else ->
//                            RectangleShape
//                    }
//                ) {
//                    recipes.forEach { recipe ->
//                        RecipeCard(
//                            recipe = recipe,
//                            isFavorite = recipe.isFavorite,
//                            onToggleFavorite = {
//                                viewModel.toggleFavorite(recipe.id, !recipe.isFavorite)
//                            },
//                            navController = navController,
//                            onDelete = { viewModel.deleteRecipe(recipe.id) },
//                            onEdit = { navController.navigate("addEditRecipe/${recipe.id}") }
//                        )
//                        Spacer(Modifier.height(8.dp))
//                    }
//                }
//
//                if (index < recipes.lastIndex) {
//                    Divider(
//                        modifier = Modifier.padding(horizontal = 16.dp),
//                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
//                    )
//                }
//            }
//        }
    }
}
