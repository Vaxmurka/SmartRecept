// ui/screens/RecipeScreen.kt
@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.ui.components.CollapsibleCard
import com.example.smartrecept.ui.components.CustomCard


@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    repository: UserPreferencesRepository,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())

    val viewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    // Загружаем конкретный рецепт
    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    val state by viewModel.recipeState.collectAsState()

    when (state) {
        is RecipeState.Loading -> LoadingScreen()
        is RecipeState.Error -> ErrorScreen()
        is RecipeState.Success -> {
            val recipe = (state as RecipeState.Success).recipe
            RecipeDetailContent(
                recipe = recipe,
                navController = navController,
                onUpdateFavorite = {id, isFavorite ->
                    viewModel.toggleFavorite(id, isFavorite)
                },
                onCookedToggle = { isCooked ->
                    viewModel.toggleCookedStatus(recipe.id, isCooked)
                }
            )
        }
    }
}

@Composable
fun LoadingScreen() {

}

@Composable
fun ErrorScreen() {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    navController: NavHostController,
    onUpdateFavorite: (Int, Boolean) -> Unit,
    onCookedToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = recipe.title,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onUpdateFavorite(recipe.id, !recipe.isFavorite)
                        }
                    ) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        var servingCoefficient by remember { mutableFloatStateOf(1f) } // Коэффициент изменения
        Column(
            modifier = modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            CustomCard(
                boxPadding = PaddingValues(bottom = 16.dp),
                outPadding = PaddingValues(bottom = 4.dp),
                shape = RoundedCornerShape(bottomEnd = 28.dp, bottomStart = 28.dp)
            ) {
                Column {
                    // Картинка рецепта
                    AsyncImage(
                        model = recipe.image,
                        placeholder = ColorPainter(Color.LightGray),
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Мета-информация
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Icon(
                                    Icons.Default.Timelapse,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${recipe.time} мин", color = MaterialTheme.colorScheme.onSecondary)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            border = BorderStroke(
                                color = Color.Transparent,
                                width = 1.dp,
                            )
                        )

                        fun calcServings(count: Int): String {
                            return when(count) {
                                1 -> "порция"
                                2, 3, 4 -> "порции"
                                else -> "порций"
                            }
                        }

                        var expanded by remember { mutableStateOf(false) }
                        var selectedServings by remember { mutableIntStateOf(recipe.servings) }
                        val portionOptions = remember(selectedServings) { getPortionOptions(selectedServings) }
                        var servingsStr = calcServings(selectedServings)
                        LaunchedEffect(selectedServings) { servingsStr = calcServings(selectedServings) }

                        Box {
                            AssistChip(
                                onClick = { expanded = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PeopleAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onTertiary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "$selectedServings $servingsStr",
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = BorderStroke(
                                    color = Color.Transparent,
                                    width = 1.dp,
                                ),
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать порции",
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            )

                            // Выпадающее меню с вариантами
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(120.dp)
                            ) {
                                // Текущее значение (неактивное)
                                DropdownMenuItem(
                                    onClick = {},
                                    text = {
                                        Text(
                                            "$selectedServings порц. (текущее)",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    },
                                    enabled = false
                                )

                                Divider()

                                // Варианты изменения
                                portionOptions.forEach { option ->
                                    DropdownMenuItem(
                                        onClick = {
                                            servingCoefficient =
                                                option.toFloat() / recipe.servings.toFloat()
                                            selectedServings = option
                                            expanded = false
                                        },
                                        text = {
                                            Text("$option порц.")
                                            Spacer(Modifier.width(8.dp))
                                        }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = {
                                    navController.navigate("addEditRecipe/${recipe.id}")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit"
                                )
                            }
                        }
                    }
                }
            }

            // Кнопка "Приготовил"
            CustomCard(
                boxPadding = PaddingValues(horizontal = 25.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = if (recipe.isCooked) "Опыт в приготовлении уже есть" else "Будем делать впервые",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        IconButton(
                            onClick = { onCookedToggle(!recipe.isCooked) },
                        ) {
                            Icon(
                                imageVector = if (recipe.isCooked) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "Done",
                                tint = if (recipe.isCooked) Color.Green else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    if (recipe.notes.isNotEmpty()) {
                        CollapsibleCard(
                            title = "Заметки",
                            initiallyExpanded = true,
                            outPadding = PaddingValues(bottom = 16.dp, top = 4.dp)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                recipe.notes.forEach { note ->
                                    Row(modifier = Modifier.padding(vertical = 0.dp).clickable(onClick = {})) {
                                        Text(
                                            "• $note",
                                            modifier = Modifier.fillMaxWidth().height(30.dp).padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Секция ингредиентов
            CustomCard(
                modifier = Modifier.fillMaxWidth(),
                boxPadding = PaddingValues(vertical = 30.dp, horizontal = 25.dp)
            ) {
                Column {
                    Text(
                        "Ингредиенты",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipe.ingredients.forEach { ingredient ->
                            Text(
                                text = "• ${
                                    if (servingCoefficient != 1f) scaleIngredient(ingredient, servingCoefficient)
                                    else ingredient
                                }",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Секция шагов приготовления
            CustomCard(boxPadding = PaddingValues(vertical = 30.dp, horizontal = 25.dp)) {
                Column {
                    Text(
                        "Способ приготовления",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        recipe.steps.forEachIndexed { index, step ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(text = step)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {navController.navigate("recipe/cook/${recipe.id}")},
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Начать готовить")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// Функция для получения вариантов изменения порций
fun getPortionOptions(current: Int): List<Int> {
    return when {
        current <= 1 -> listOf(current * 2, current * 3, current * 4)
        current <= 2 -> listOf(current / 2, current * 2, current * 3)
        current <= 4 -> listOf(current / 2, current + 2, current * 2)
        current <= 10 -> listOf(current / 2, current - 2, current + 2, current * 2, current * 3)
        else -> listOf(current / 2, current - 5, current + 5, current * 2)
    }.distinct()
        .filter { it > 0 && it != current }
        .sorted()
}


// Функция для масштабирования количества ингредиента
fun scaleIngredient(ingredient: String, coefficient: Float): String {
    return ingredient.replace(Regex("""(\d+\.?\d*)(\s*[а-яa-z.]+)""")) {
        val (amount, unit) = it.destructured
        val scaled = amount.toFloat() * coefficient
        "%.${if (scaled % 1 == 0f) 0 else 1}f$unit".format(scaled)
    }
}