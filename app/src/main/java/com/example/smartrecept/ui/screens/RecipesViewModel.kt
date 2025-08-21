// ui/screens/RecipeViewModel.kt
package com.example.smartrecept.ui.screens

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.smartrecept.data.recipes.DatasourceRecipes
import com.example.smartrecept.data.recipes.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class RecipeState {
    object Loading : RecipeState()
    data class Success(val recipe: Recipe) : RecipeState()
    object Error : RecipeState()
}

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = DatasourceRecipes(application.applicationContext)

    // Для списка рецептов
    private var _recipes = dataSource.loadRecipesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val recipes: StateFlow<List<Recipe>> = _recipes

    // Для состояния загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Для экрана одного рецепта
    private val _recipeState = MutableStateFlow<RecipeState>(RecipeState.Loading)
    val recipeState: StateFlow<RecipeState> = _recipeState

    init {
        loadRecipes()
    }

    fun loadRecipe(id: Int) {
        viewModelScope.launch {
            _recipeState.value = RecipeState.Loading
            try {
                val recipe = dataSource.getRecipeById(id) ?: throw Exception("Recipe not found")
                _recipeState.value = RecipeState.Success(recipe)
            } catch (e: Exception) {
                _recipeState.value = RecipeState.Error
            }
        }
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                dataSource.initializeDatabase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(recipeId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            // 1. Обновляем в базе данных
            dataSource.updateFavoriteRecipe(recipeId, isFavorite)

            // 2. Обновляем состояние текущего рецепта, если мы на его экране
            _recipeState.value.let { currentState ->
                if (currentState is RecipeState.Success && currentState.recipe.id == recipeId) {
                    _recipeState.value = RecipeState.Success(
                        currentState.recipe.copy(isFavorite = isFavorite)
                    )
                }
            }
        }
    }

    private val _recipeOfTheDay = mutableStateOf<Recipe?>(null)
    val recipeOfTheDay: State<Recipe?> = _recipeOfTheDay

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    fun updateRecipeOfTheDay(recipes: List<Recipe>) {
        _recipeOfTheDay.value = if (recipes.isNotEmpty()) recipes.random() else null
    }

    fun refreshRecipe(recipes: List<Recipe>) {
        _isRefreshing.value = true
        viewModelScope.launch {
            updateRecipeOfTheDay(recipes)
            _isRefreshing.value = false
        }
    }

    fun saveRecipe(
        id: Int?,
        title: String,
        tags: Set<String>,
        time: String,
        image: String?,
        servings: Int,
        ingredients: List<String>,
        steps: List<String>,
        stepImages: List<String?>,
        notes: List<String>,
        navController: NavHostController
    ) {
        viewModelScope.launch {
            try {
                if (id == null) {
                    // Создание нового рецепта
                    val newRecipe = Recipe(
                        title = title,
                        tags = tags.toList(),
                        time = time,
                        image = image ?: "",
                        servings = servings,
                        isFavorite = false,
                        ingredients = ingredients.filter { it.isNotBlank() },
                        steps = steps.filter { it.isNotBlank() },
                        stepImages = stepImages,
                        notes = notes.filter { it.isNotBlank() }
                    )
                    dataSource.insertRecipe(newRecipe)
                } else {
                    // Обновление существующего
                    val updatedRecipe = Recipe(
                        id = id,
                        title = title,
                        tags = tags.toList(),
                        time = time,
                        image = image ?: "",
                        servings = servings,
                        isFavorite = dataSource.isFavorite(id),
                        ingredients = ingredients.filter { it.isNotBlank() },
                        steps = steps.filter { it.isNotBlank() },
                        stepImages = stepImages,
                        notes = notes.filter { it.isNotBlank() }
                    )
                    dataSource.updateRecipe(updatedRecipe)
                }
                navController.popBackStack()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun deleteRecipe(recipeId: Int) {
        viewModelScope.launch {
            dataSource.deleteRecipe(recipeId)
        }
    }


    // Добавляем StateFlow для избранных рецептов
    private val _favorites = dataSource.getFavoritesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val favorites: StateFlow<List<Recipe>> = _favorites

    fun clearAllFavorites() {
        viewModelScope.launch {
            dataSource.clearAllFavorites()
            // Можно добавить обновление списка, если нужно
        }
    }


    private val _cookedRecipes = dataSource.getCookedRecipesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val cookedRecipes: StateFlow<List<Recipe>> = _cookedRecipes

    fun toggleCookedStatus(recipeId: Int, isCooked: Boolean) {
        viewModelScope.launch {
            // 1. Обновляем в базе данных
            dataSource.updateCookedStatus(recipeId, isCooked)

            // 2. Обновляем состояние текущего рецепта, если мы на его экране
            _recipeState.value.let { currentState ->
                if (currentState is RecipeState.Success && currentState.recipe.id == recipeId) {
                    _recipeState.value = RecipeState.Success(
                        currentState.recipe.copy(isCooked = isCooked)
                    )
                }
            }
        }
    }

    // Добавляем в RecipeViewModel
    fun saveCookingNotes(
        recipeId: Int,
        newNotes: List<String>,
        keepPrevious: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Получаем текущий рецепт
                val currentRecipe = dataSource.getRecipeById(recipeId)

                currentRecipe?.let { recipe ->
                    // Формируем итоговые заметки
                    val updatedNotes = when {
                        newNotes.isEmpty() -> recipe.notes
                        keepPrevious -> recipe.notes + newNotes
                        else -> newNotes
                    }.filter { it.isNotBlank() } // Фильтруем пустые строки

                    // Обновляем в базе
                    dataSource.updateRecipeNotes(recipeId, updatedNotes)

                    // Обновляем состояние, если этот рецепт сейчас открыт
                    if (_recipeState.value is RecipeState.Success &&
                        (_recipeState.value as RecipeState.Success).recipe.id == recipeId) {
                        _recipeState.value = RecipeState.Success(
                            recipe.copy(notes = updatedNotes)
                        )
                    }
                }

                onComplete()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    // Дополнительные методы по мере необходимости
}