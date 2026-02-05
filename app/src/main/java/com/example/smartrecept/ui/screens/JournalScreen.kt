// ui/screens/JournalScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartrecept.R
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.RecipeCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    repository: UserPreferencesRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    val cookedRecipes by viewModel.cookedRecipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.cooked_recipes_title),
                            modifier = Modifier.fillMaxWidth().padding(end = 20.dp),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {},
                    actions = {}
                )
            }
        ) { padding ->
            JournalScreenContent(
                recipes = cookedRecipes,
                navController = navController,
                paddingValues = padding,
                onUpdateFavorite = { id, isFavorite ->
                    viewModel.toggleFavorite(id, isFavorite)
                },
            )
        }
    }
}

@Composable
fun JournalScreenContent(
    recipes: List<Recipe>,
    navController: NavHostController,
    paddingValues: PaddingValues,
    onUpdateFavorite: (Int, Boolean) -> Unit,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 56.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        CustomCard(
            shape = RoundedCornerShape(bottomEnd = 28.dp, bottomStart = 28.dp),
            boxPadding = PaddingValues(vertical = 16.dp),
            outPadding = PaddingValues(bottom = 4.dp)
        ) {
            LazyColumn(modifier = Modifier) {
                itemsIndexed(recipes) { index, recipe ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = when {
                            index == 0 && recipes.size == 1 ->
                                RoundedCornerShape(16.dp)
                            index == 0 ->
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            index == recipes.lastIndex ->
                                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            else ->
                                RectangleShape
                        }
                    ) {
                        RecipeCard(
                            recipe = recipe,
                            isFavorite = recipe.isFavorite,
                            onToggleFavorite = { onUpdateFavorite(recipe.id, !recipe.isFavorite) },
                            navController = navController,
                            onDelete = { viewModel.deleteRecipe(recipe.id) },
                            onEdit = { navController.navigate("addEditRecipe/${recipe.id}") },
                            onCookedToggle = { isCooked ->
                                viewModel.toggleCookedStatus(recipe.id, isCooked)
                            }
                        )
                    }

                    if (index < recipes.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }

                // Показываем сообщение, если журнал пустой
                if (recipes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_cooked_recipes_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}