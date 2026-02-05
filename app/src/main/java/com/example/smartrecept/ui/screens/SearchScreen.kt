package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import ScrollHandler
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartrecept.R
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.filterChipsList
import com.example.smartrecept.ui.components.RecipeCard
import com.example.smartrecept.ui.components.getTagColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.CustomSearchPanel
import com.example.smartrecept.ui.components.FilterBottomSheet
import kotlin.collections.filter

@OptIn(ExperimentalLayoutApi::class)
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
    var showFiltersSheet by remember { mutableStateOf(false) }

    // дополнительные фильтры
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlyCooked by remember { mutableStateOf(false) }
    var maxTime by remember { mutableStateOf<Int?>(null) }

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

    // Получаем теги для фильтров
    val allTags = remember(recipes) {
        val tagFrequency = mutableMapOf<String, Int>()
        recipes.forEach { recipe ->
            recipe.tags
                .filter { it.isNotBlank() }
                .forEach { tag ->
                    tagFrequency[tag] = tagFrequency.getOrDefault(tag, 0) + 1
                }
        }

        val popularTags = tagFrequency
            .toList()
            .sortedByDescending { (_, count) -> count }
            .take(10)
            .map { (tag, _) -> tag }

        val defaultTags = filterChipsList
        (defaultTags + popularTags).distinct().take(15)
    }

    // Считаем количество активных фильтров
    val activeFiltersCount = remember(selectedFilter, onlyFavorites, onlyCooked, maxTime, useAndLogic) {
        var count = 0
        if (selectedFilter != null) count++
        if (onlyFavorites) count++
        if (onlyCooked) count++
        if (maxTime != null) count++
        if (useAndLogic) count++
        count
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
        },
        floatingActionButton = {
            // Плавающая кнопка фильтров с бейджем
            if (query.isNotBlank() && filteredRecipes.isNotEmpty()) {
                Box {
                    FloatingActionButton(
                        onClick = { showFiltersSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, shape = CircleShape)
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeFiltersCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(activeFiltersCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filters)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Показываем активные фильтры как чипсы
                if (activeFiltersCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Тег фильтр
                        selectedFilter?.let { tag ->
                            InputChip(
                                selected = true,
                                onClick = { selectedFilter = null },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = getTagColor(tag)
                                )
                            )
                        }

                        // Избранное
                        if (onlyFavorites) {
                            InputChip(
                                selected = true,
                                onClick = { onlyFavorites = false },
                                label = { Text(stringResource(R.string.favorites_title)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // Приготовленные
                        if (onlyCooked) {
                            InputChip(
                                selected = true,
                                onClick = { onlyCooked = false },
                                label = { Text(stringResource(R.string.cooked_recipes_title)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // Время
                        maxTime?.let { time ->
                            InputChip(
                                selected = true,
                                onClick = { maxTime = null },
                                label = { Text(stringResource(R.string.up_to_minutes, time)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // Режим И
                        if (useAndLogic) {
                            InputChip(
                                selected = true,
                                onClick = { useAndLogic = false },
                                label = { Text(stringResource(R.string.and_mode)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                when {
                    query.isBlank() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Заголовок с кнопкой фильтров
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.try_searching),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Кнопка фильтров
                                BadgedBox(
                                    badge = {
                                        if (activeFiltersCount > 0) {
                                            Badge {
                                                Text(activeFiltersCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    IconButton(
                                        onClick = { showFiltersSheet = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = stringResource(R.string.filters)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Подсказки
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                val suggestions = listOf(
                                    stringResource(R.string.search_suggestion_1),
                                    stringResource(R.string.search_suggestion_2),
                                    stringResource(R.string.search_suggestion_3),
                                    stringResource(R.string.search_suggestion_4),
                                    stringResource(R.string.search_suggestion_5),
                                    stringResource(R.string.search_suggestion_6)
                                )
                                suggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { query = suggestion },
                                        label = { Text(suggestion) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Или покажем популярные теги как фильтры
                            if (allTags.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.popular_tags),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    allTags.take(8).forEach { tag ->
                                        AssistChip(
                                            onClick = { selectedFilter = if (selectedFilter == tag) null else tag },
                                            label = { Text(tag) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (selectedFilter == tag)
                                                    getTagColor(tag)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    filteredRecipes.isNotEmpty() -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.recipes_found, filteredRecipes.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val listState = rememberLazyListState()

                        LaunchedEffect(listState.isScrollInProgress) {
                            if (listState.isScrollInProgress) {
                                scrollHandler.handleLazyListScroll(listState)
                            }
                        }

                        LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } }) {
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
                        // если ничего не найдено
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = stringResource(R.string.nothing_found),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.nothing_found),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.try_changing_query),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Кнопка фильтров в экране "ничего не найдено"
                            Button(
                                onClick = { showFiltersSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.configure_filters))
                                if (activeFiltersCount > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge {
                                        Text(activeFiltersCount.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet с фильтрами
        FilterBottomSheet(
            isVisible = showFiltersSheet,
            onDismiss = { showFiltersSheet = false },
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            onlyFavorites = onlyFavorites,
            onOnlyFavoritesChange = { onlyFavorites = it },
            onlyCooked = onlyCooked,
            onOnlyCookedChange = { onlyCooked = it },
            maxTime = maxTime,
            onMaxTimeChange = { maxTime = it },
            useAndLogic = useAndLogic,
            onUseAndLogicChange = { useAndLogic = it },
            tags = allTags
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularRecipesSection(
    recipes: List<Recipe>,
    navController: NavHostController,
    viewModel: RecipeViewModel
) {
    if (recipes.isEmpty()) return

    CustomCard(
        shape = RoundedCornerShape(28.dp),
        boxPadding = PaddingValues(top = 16.dp, bottom = 4.dp),
        outPadding = PaddingValues(bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.popular),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
    }
}

@Composable
fun FilterChips(
    selected: String?,
    onSelect: (String?) -> Unit,
    tags: List<String> = filterChipsList
) {
    val options = tags
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