// ui/screens/CookingScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.smartrecept.data.recipes.CookingStep
import com.example.smartrecept.data.recipes.Recipe
import com.example.smartrecept.data.recipes.toEnhancedCookingSteps
import com.example.smartrecept.data.settings.UserPreferences
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.ui.components.CameraScreen
import com.example.smartrecept.ui.components.CollapsibleCard
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import java.io.File


@Composable
fun CookingScreen(
    recipeId: Int,
    repository: UserPreferencesRepository,
    navController: NavHostController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())

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
            CookingScreenContent(
                recipe = recipe,
                navController = navController
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingScreenContent(
    recipe: Recipe,
    navController: NavHostController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = recipe.title,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            CollapsibleCard(
                title = "Ингредиенты",
                initiallyExpanded = true
            ) {
                Column {
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            text = "• $ingredient",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Блок с заметками (изначально скрыт)
            if (recipe.notes.isNotEmpty()) {
                CollapsibleCard(
                    title = "Заметки",
                    initiallyExpanded = false,
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

            val cookingSteps = recipe.toEnhancedCookingSteps()

            CookingStepScreen(
                recipe = recipe,
                steps = cookingSteps,
                onBack = { notes, keepPrevious ->
                    viewModel.toggleCookedStatus(recipe.id, true)

                    val currentRecipe = viewModel.recipeState.value
                    if (currentRecipe is RecipeState.Success) {
                        viewModel.saveCookingNotes(
                            recipeId = currentRecipe.recipe.id,
                            newNotes = listOf(notes),
                            keepPrevious = keepPrevious,
                            onComplete = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    } else {
                        navController.navigate("home")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun CookingStepScreen(
    recipe: Recipe,
    steps: List<CookingStep>,
    onBack: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Делаем шаги наблюдаемыми
    val stepStateList = remember {
        mutableStateListOf(*steps.toTypedArray())
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = stepStateList[currentStepIndex]

    // Обновляем шаг
    fun updateStep(newStep: CookingStep) {
        stepStateList[currentStepIndex] = newStep
    }

    var showEndDialog by remember { mutableStateOf(false) }
    var cookingNotes by remember { mutableStateOf("") }
    var keepPreviousNotes by remember { mutableStateOf(true) }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Завершить готовку") },
            text = {
                Column {
                    if (recipe.notes.isNotEmpty()) {
                        // Блок заметок с растягиванием на всю ширину
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                recipe.notes.forEach { note ->
                                    Text(
                                        "• $note",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Если у вас появились замечания, оставьте их ниже:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cookingNotes,
                        onValueChange = { cookingNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Мои заметки о готовке...") },
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Переключатель сохранения предыдущих записей
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { keepPreviousNotes = !keepPreviousNotes }
                    ) {
                        Checkbox(
                            checked = keepPreviousNotes,
                            onCheckedChange = { keepPreviousNotes = it }
                        )
                        Text(
                            text = "Сохранить предыдущие записи",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBack(cookingNotes, keepPreviousNotes) // Boolean -> флаг на созранение или удаление прошлых записей
                        showEndDialog = false
                        cookingNotes = "" // Очищаем поле
                    }
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    cookingNotes = "" // Очищаем поле при отмене
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя панель прогресса
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Шаг ${currentStepIndex + 1}/${stepStateList.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / stepStateList.size },
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Контент шага
            Column(modifier = Modifier) {
                // Фото шага
                currentStep.imageUrl?.let {
                    Log.d("COOK_INFO", it)
                    val model: Any = if (currentStep.imageUrl.startsWith("/")) {
                        Uri.fromFile(File(currentStep.imageUrl))
                    } else {
                        currentStep.imageUrl // это может быть http(s) или content:// (для старых записей)
                    }

                    AsyncImage(
                        model = model,
                        contentDescription = "Шаг ${currentStepIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Таймер, если есть
                currentStep.timerMinutes?.let { minutes ->
                    StepTimer(
                        timerMinutes = minutes,
                        isTimerRunning = currentStep.isTimerRunning,
                        onTimerToggle = {
                            updateStep(currentStep.copy(isTimerRunning = !currentStep.isTimerRunning))
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Кнопки навигации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentStepIndex > 0) currentStepIndex--
                    },
                    enabled = currentStepIndex > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Предыдущий шаг")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (currentStepIndex < stepStateList.lastIndex) {
                            currentStepIndex++
                        } else {
                            showEndDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (currentStepIndex == stepStateList.lastIndex)
                            "Завершить" else "Следующий шаг"
                    )
                }
            }
        }

        // Сброс таймера при смене шага
        LaunchedEffect(currentStepIndex) {
            updateStep(stepStateList[currentStepIndex].copy(isTimerRunning = false))
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StepTimer(
    timerMinutes: Int,
    isTimerRunning: Boolean,
    onTimerToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember { mutableIntStateOf(timerMinutes * 60) }

    LaunchedEffect(isTimerRunning, timerMinutes) {
        // Сбрасываем таймер при изменении времени или шага
        remainingSeconds = timerMinutes * 60

        // Запускаем/останавливаем отсчет
        if (isTimerRunning) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            onTimerToggle() // автоматически останавливаем по завершении
        }
    }

    val formattedTime = remember(remainingSeconds) {
        String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTimerToggle() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isTimerRunning) Icons.Filled.Timer else Icons.Outlined.Timer,
            contentDescription = "Таймер",
            tint = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formattedTime,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isTimerRunning) "Таймер запущен" else "Нажмите для запуска",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}