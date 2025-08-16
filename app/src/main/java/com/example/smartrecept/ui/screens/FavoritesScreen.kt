// ui/screens/FavoritesScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartrecept.data.settings.UserPreferencesRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.ui.components.CustomCard
import com.example.smartrecept.ui.components.RecipeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repository: UserPreferencesRepository,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())

    val viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application))
    val favorites by viewModel.favorites.collectAsState()
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
                            text = "Избранное",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 64.dp), // Сдвигает текст вправо
                            textAlign = TextAlign.Center, // Выравнивание текста внутри Text
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {}
                )
            }
        ) { padding ->
            FavoritesView(
                recipes = favorites,
                onUpdateFavorite = { id, isFavorite ->
                    viewModel.toggleFavorite(id, isFavorite)
                },
                paddingValues = padding,
                navController = navController
            )
        }

    }
}

@Composable
fun FavoritesView(
    recipes: List<Recipe>,
    onUpdateFavorite: (Int, Boolean) -> Unit,
    navController: NavHostController,
    paddingValues: PaddingValues,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    Box(Modifier.fillMaxSize().padding(top = 56.dp), contentAlignment = Alignment.TopCenter) {
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
                            onEdit = { navController.navigate("addEditRecipe/${recipe.id}") }
                        )
                    }

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
}

@Preview(showBackground = true)
@Composable
fun FavoritesPreview() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userPrefsRepo = remember { UserPreferencesRepository(context) }

    FavoritesScreen(repository = userPrefsRepo, navController = navController)
}