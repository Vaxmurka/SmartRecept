package com.example.smartrecept.ui.components

import AIRecipe
import RecipeViewModelFactory
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartrecept.BuildConfig
import com.example.smartrecept.ui.screens.RecipeViewModel
import com.example.smartrecept.ui.screens.getPortionOptions
import com.example.smartrecept.ui.screens.scaleIngredient
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(PublicPreviewAPI::class)
@Composable
fun AIassistLogic(
    navController: NavController,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val prompt_preparing_visual = """
        ВЫВЕДИ ИНФОРМАЦИЮ В ФОРМАТЕ JSON !!!ТОЛЬКО JSON!!! ПО СЛЕДУЮЩЕМУ АЛГОРИТМУ:
        
        1. В блоке 'airecipe_name' укажи название рецепта (тип String)
        2. В блоке 'ingredients' перечисли ингредиенты в виде массива (списка) [ингредиент1, ингредиент2]
        3. В блоке 'tags' перечисли теги в виде массива (списка) [тег1, тег2, тег3]
        4. В блоке 'time' укажи примерное время приготовления в минутах (тип String)
        5. В блоке 'servings' укажи количество порций (тип Int)
        6. В блоке 'steps' напиши шаги приготовления рецепта подробно
        7. В блоке 'notes' если требуется, добавь заметки или подсказки
        8. В блоке 'image_url' добавь РЕАЛЬНУЮ ССЫЛКУ на фотографию готового блюда из интернета в формате JPG/PNG.
           Фото должно быть аппетитным, хорошо освещенным, высокого качества.
           Пример правильной ссылки: https://gipfel.ru/upload/iblock/6a3/0h4yv2q51p0y6md8a1w4c5zjfsuc3dod.jpg
           
        ВАЖНО: Фото должно максимально точно соответствовать рецепту!
    """.trimIndent()

    var prompt by remember { mutableStateOf("Напиши рецепт омлета") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedModel by remember { mutableStateOf(AiModel.GEMINI) }

    var parsedRecipe by remember { mutableStateOf<AIRecipe?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Кнопка назад
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("← Назад")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Вопрос для Gemini") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                response = "" // Очищаем предыдущий ответ
                parsedRecipe = null // Сбрасываем спарсенный рецепт
                scope.launch {
                    try {
                        val result = askAI(prompt + prompt_preparing_visual, selectedModel)
                        response = result
                        println("✅ Получен ответ: ${if (result.length > 50) result.substring(0, 50) + "..." else result}")

                        // Парсим рецепт после получения ответа
                        if (isValidJsonResponse(result)) {
                            parsedRecipe = AIJsonParser.parseAIRecipe(result)
                        }
                    } catch (e: Exception) {
                        response = "ОШИБКА: ${e.message}"
                        println("❌ Ошибка: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && prompt.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            if (isLoading) {
                Text("Загружаю...")
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Спросить у ${selectedModel.name}")
            }
        }

        // Выбор модели
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedModel == AiModel.GEMINI,
                onClick = { selectedModel = AiModel.GEMINI },
                label = { Text("Gemini") }
            )
            FilterChip(
                selected = selectedModel == AiModel.TESTER,
                onClick = { selectedModel = AiModel.TESTER },
                label = { Text("Tester") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Показываем результат или загрузку
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ИИ генерирует рецепт...")
                    }
                }
            }

            response.isNotEmpty() -> {
                // Проверяем, валидный ли JSON получен
                if (isValidJsonResponse(response)) {
                    Column {
                        // Кнопка сохранения (только если рецепт спарсен)
                        if (parsedRecipe != null) {
                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("💾 Сохранить рецепт в БД")
                            }
                        }

                        // Превью
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            PreviewRecipe(response)
                        }
                    }
                } else {
                    // Показываем текст ответа, если это не JSON
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Ответ не в формате JSON",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = response,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            else -> {
                // Начальный экран с инструкцией
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI Кулинарный Ассистент",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Напишите запрос для генерации рецепта, например:\n" +
                                    "• Напиши рецепт омлета\n" +
                                    "• Как приготовить пасту карбонара\n" +
                                    "• Рецепт борща с говядиной\n" +
                                    "• Вегетарианский салат с авокадо",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Диалог сохранения
    if (showSaveDialog && parsedRecipe != null) {
        SaveRecipeDialog(
            aiRecipe = parsedRecipe!!,
            onSave = { recipeTitle ->
                scope.launch {
                    try {
                        // Используем метод ViewModel для сохранения
                        val result = saveAIRecipeToDatabase(
                            aiRecipe = parsedRecipe!!.copy(airecipe_name = recipeTitle),
                            viewModel = viewModel
                        )

                        if (result.isSuccess) {
                            Toast.makeText(
                                context,
                                "Рецепт '$recipeTitle' сохранен!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Закрываем диалог
                            showSaveDialog = false

                            // Можно вернуться назад или очистить форму
                            // navController.popBackStack()
                        } else {
                            Toast.makeText(
                                context,
                                "Ошибка сохранения: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onCancel = { showSaveDialog = false }
        )
    }
}

@Composable
fun SaveRecipeDialog(
    aiRecipe: AIRecipe,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var recipeTitle by remember { mutableStateOf(aiRecipe.airecipe_name) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Сохранение рецепта") },
        text = {
            Column {
                Text("Рецепт будет сохранен в вашу базу данных.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = recipeTitle,
                    onValueChange = {
                        recipeTitle = it
                        titleError = false
                    },
                    label = { Text("Название рецепта") },
                    isError = titleError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (titleError) {
                    Text(
                        "Введите название рецепта",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Информация о рецепте
                Spacer(modifier = Modifier.height(16.dp))
                Text("Информация о рецепте:", fontWeight = FontWeight.Bold)
                Text("• ${aiRecipe.ingredients.size} ингредиентов")
                Text("• ${aiRecipe.steps.size} шагов приготовления")
                Text("• Время: ${aiRecipe.time} минут")
                Text("• Порций: ${aiRecipe.servings}")
                if (aiRecipe.tags.isNotEmpty()) {
                    Text("• Теги: ${aiRecipe.tags.joinToString(", ")}")
                }
                if (!aiRecipe.image_url.isNullOrBlank()) {
                    Text("• 📸 Изображение: будет добавлено", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (recipeTitle.isBlank()) {
                        titleError = true
                    } else {
                        onSave(recipeTitle)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Отмена")
            }
        }
    )
}

// Упрощенная функция сохранения AI рецепта
suspend fun saveAIRecipeToDatabase(
    aiRecipe: AIRecipe,
    viewModel: RecipeViewModel
): Result<Unit> {
    return try {
        viewModel.saveAIRecipe(
            title = aiRecipe.airecipe_name,
            tags = aiRecipe.tags,
            time = aiRecipe.time,
            servings = aiRecipe.servings,
            ingredients = aiRecipe.ingredients.filter { it.isNotBlank() },
            steps = aiRecipe.steps.filter { it.isNotBlank() },
            notes = aiRecipe.notes.filter { it.isNotBlank() },
            image = aiRecipe.image_url, // Передаем URL изображения
            onSuccess = { /* Успех уже обрабатывается в AIassistLogic */ },
            onError = { error -> throw Exception(error) }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewRecipe(jsonRecipe: String) {
    println("=== ПРЕВЬЮ РЕЦЕПТА ===")
    println("Исходный текст: ${if (jsonRecipe.length > 100) jsonRecipe.substring(0, 100) + "..." else jsonRecipe}")

    // Используем safe парсинг
    val recipe = remember {
        try {
            AIJsonParser.parseAIRecipe(jsonRecipe)
        } catch (e: Exception) {
            AIRecipe(
                airecipe_name = "Ошибка загрузки",
                ingredients = emptyList(),
                tags = emptyList(),
                time = "0",
                servings = 1,
                steps = emptyList(),
                notes = emptyList(),
                image_url = null
            )
        }
    }

    println("✅ Распарсено: ${recipe.airecipe_name}")
    println("📸 URL изображения: ${recipe.image_url ?: "Нет изображения"}")
    println("======================")

    var servingCoefficient by remember { mutableFloatStateOf(1f) }
    var selectedServings by remember { mutableIntStateOf(recipe.servings) }

    // Используем Column вместо LazyColumn для избежания ошибок с высотой
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Изображение рецепта (если есть)
        if (!recipe.image_url.isNullOrBlank()) {
            CustomCard(
                modifier = Modifier.fillMaxWidth(),
                boxPadding = PaddingValues(0.dp)
            ) {
                AsyncImage(
                    model = recipe.image_url,
                    contentDescription = "Изображение рецепта",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = ColorPainter(Color.LightGray),
                    error = ColorPainter(Color.LightGray)
                )
            }
        }

        // 1. Шапка с названием и информацией
        CustomCard(
            boxPadding = PaddingValues(bottom = 16.dp),
            outPadding = PaddingValues(bottom = 4.dp),
            shape = RoundedCornerShape(bottomEnd = 28.dp, bottomStart = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Название рецепта
                Text(
                    text = recipe.airecipe_name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Мета-информация
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Время приготовления
                    AssistChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timelapse,
                                    contentDescription = "Время",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${recipe.time} мин",
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            color = Color.Transparent,
                            width = 1.dp,
                        )
                    )

                    // Количество порций с выбором
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        AssistChip(
                            onClick = { expanded = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PeopleAlt,
                                        contentDescription = "Порции",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "$selectedServings ${getPortionText(selectedServings)}",
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

                        // Выпадающее меню
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(140.dp)
                        ) {
                            // Варианты порций
                            val portionOptions = getPortionOptions(selectedServings)

                            portionOptions.forEach { option ->
                                DropdownMenuItem(
                                    onClick = {
                                        servingCoefficient = option.toFloat() / recipe.servings.toFloat()
                                        selectedServings = option
                                        expanded = false
                                    },
                                    text = {
                                        Text("$option ${getPortionText(option)}")
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Теги рецепта
                if (recipe.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        recipe.tags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text("#$tag") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 2. Заметки (если есть)
        if (recipe.notes.isNotEmpty()) {
            CustomCard(
                boxPadding = PaddingValues(horizontal = 25.dp, vertical = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Заголовок
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Заметки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Список заметок
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recipe.notes.forEachIndexed { index, note ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = note,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            if (index < recipe.notes.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Ингредиенты
        CustomCard(
            modifier = Modifier.fillMaxWidth(),
            boxPadding = PaddingValues(vertical = 20.dp, horizontal = 25.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Ингредиенты",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (servingCoefficient != 1f) {
                        Text(
                            "${String.format("%.1f", servingCoefficient)}x",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recipe.ingredients.forEachIndexed { index, ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (servingCoefficient != 1f) {
                                    scaleIngredient(ingredient, servingCoefficient)
                                } else {
                                    ingredient
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < recipe.ingredients.size - 1) {
                            Divider(
                                modifier = Modifier.padding(start = 32.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Шаги приготовления
        recipe.steps.forEachIndexed { index, step ->
            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                boxPadding = PaddingValues(vertical = 20.dp, horizontal = 25.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Номер шага
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Шаг ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Текст шага
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. Пустое пространство внизу
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Функция проверки валидности JSON ответа
fun isValidJsonResponse(response: String): Boolean {
    return try {
        // Убираем возможный лишний текст до и после JSON
        val cleanResponse = response.trim()

        // Проверяем, начинается ли ответ с { и заканчивается }
        if (!cleanResponse.startsWith("{") || !cleanResponse.endsWith("}")) {
            return false
        }

        // Пробуем парсить JSON
        val jsonStart = cleanResponse.indexOf('{')
        val jsonEnd = cleanResponse.lastIndexOf('}') + 1
        val jsonString = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleanResponse.substring(jsonStart, jsonEnd)
        } else {
            cleanResponse
        }

        JSONObject(jsonString)
        true
    } catch (e: Exception) {
        println("❌ Невалидный JSON: ${e.message}")
        false
    }
}

// Вспомогательная функция для текста порций
@Composable
fun getPortionText(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "порция"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "порции"
        else -> "порций"
    }
}

enum class AiModel {
    GEMINI, TESTER
}

suspend fun askAI(prompt: String, model: AiModel = AiModel.GEMINI): String {
    return when (model) {
        AiModel.GEMINI -> askGemini(prompt)
        AiModel.TESTER -> askTester(prompt)
    }
}

@OptIn(PublicPreviewAPI::class)
suspend fun askGemini(prompt: String): String {
    return try {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")

        val strictPrompt = """
            ТВОЯ ЗАДАЧА: Создать рецепт и добавить РЕАЛЬНУЮ ССЫЛКУ НА ИЗОБРАЖЕНИЕ из TheMealDB API.
            
            $prompt
            
            ДЛЯ ПОЛУЧЕНИЯ ИЗОБРАЖЕНИЯ ИСПОЛЬЗУЙ ЭТУ ССЫЛКУ:
            Если рецепт похож на одно из этих блюд, используй соответствующую ссылку:
            
            • Омлет, яичница → https://www.themealdb.com/images/media/meals/ysxwuq1487323065.jpg
            • Паста, спагетти → https://www.themealdb.com/images/media/meals/sutysw1468247559.jpg
            • Пицца → https://www.themealdb.com/images/media/meals/x0lk931587671540.jpg
            • Бургер → https://www.themealdb.com/images/media/meals/urzj1d1587670726.jpg
            • Суп → https://www.themealdb.com/images/media/meals/1529445434.jpg
            • Салат → https://www.themealdb.com/images/media/meals/1549542877.jpg
            • Торт, десерт → https://www.themealdb.com/images/media/meals/yypvst1511386427.jpg
            • Курица → https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg
            • Рыба → https://www.themealdb.com/images/media/meals/uwxusv1487344500.jpg
            • Рис → https://www.themealdb.com/images/media/meals/1520081754.jpg
            
            Если блюдо не подходит под эти категории, используй дефолтное изображение:
            https://www.themealdb.com/images/media/meals/1529446352.jpg
            
            ФОРМАТ ОТВЕТА ТОЛЬКО JSON:
            {
                "airecipe_name": "Название рецепта",
                "ingredients": ["ингредиент1", "ингредиент2"],
                "tags": ["основной_тег", "дополнительный_тег"],
                "time": "30",
                "servings": 2,
                "steps": ["Шаг 1", "Шаг 2"],
                "notes": ["Заметка"],
                "image_url": "СЮДА ВСТАВЬ ОДНУ ИЗ ССЫЛОК ВЫШЕ"
            }
            
            ПРИМЕР:
            {
                "airecipe_name": "Классический омлет",
                "ingredients": ["3 яйца", "100 мл молока", "соль", "перец"],
                "tags": ["омлет", "завтрак", "быстро"],
                "time": "10",
                "servings": 2,
                "steps": ["Взбейте яйца с молоком", "Жарьте на сковороде"],
                "notes": ["Подавайте горячим"],
                "image_url": "https://www.themealdb.com/images/media/meals/ysxwuq1487323065.jpg"
            }
        """.trimIndent()

        println("📤 Отправляю запрос Gemini с запросом на изображение...")
        val result = model.generateContent(strictPrompt)

        val responseText = result.text?.trim() ?: "{}"
        println("📥 Получен ответ с изображением: ${responseText.take(300)}...")

        // Очищаем ответ от возможного markdown
        val cleaned = cleanGeminiResponse(responseText)
        println("🧹 Очищенный ответ: ${cleaned.take(300)}...")

        cleaned

    } catch (e: Exception) {
        println("❌ Ошибка Gemini: ${e.message}")
        // Возвращаем JSON без изображения при ошибке
        """{
            "airecipe_name": "Ошибка загрузки",
            "ingredients": [],
            "tags": [],
            "time": "0",
            "servings": 1,
            "steps": [],
            "notes": ["Ошибка: ${e.message?.take(50) ?: "Неизвестная ошибка"}"],
            "image_url": null
        }"""
    }
}

suspend fun askTester(prompt: String): String {
    return """
    {
      "airecipe_name": "Классический омлет с сыром",
      "ingredients": ["3 яйца", "100 мл молока", "соль", "перец", "50 г сыра", "1 ст.л. масла"],
      "tags": ["завтрак", "быстро", "яйца"],
      "time": "15",
      "servings": 2,
      "steps": [
        "Взбейте яйца с молоком до однородности",
        "Добавьте соль, перец и натертый сыр",
        "Разогрейте сковороду с маслом",
        "Вылейте смесь и жарьте на среднем огне 5-7 минут",
        "Подавайте горячим с зеленью"
      ],
      "notes": ["Для пышности можно добавить щепотку соды", "Сыр можно заменить на любой другой"],
      "image_url": "https://images.unsplash.com/photo-1551024709-8f23befc6f87"
    }
    """.trimIndent()
}

// Функция для очистки ответа Gemini
private fun cleanGeminiResponse(response: String): String {
    var cleaned = response.trim()

    // Убираем markdown блоки
    if (cleaned.startsWith("```json")) {
        cleaned = cleaned.substringAfter("```json").trim()
    }
    if (cleaned.startsWith("```")) {
        cleaned = cleaned.substringAfter("```").trim()
    }
    if (cleaned.endsWith("```")) {
        cleaned = cleaned.substringBeforeLast("```").trim()
    }

    // Находим начало и конец JSON
    val jsonStart = cleaned.indexOf('{')
    val jsonEnd = cleaned.lastIndexOf('}') + 1

    return if (jsonStart >= 0 && jsonEnd > jsonStart) {
        cleaned.substring(jsonStart, jsonEnd)
    } else {
        cleaned
    }
}

// Функция для получения изображения на основе тегов рецепта
fun getFoodImageByTags(tags: List<String>): String {
    if (tags.isEmpty()) return getDefaultFoodImage()

    val mainTag = tags.first().lowercase()

    // Карта тегов к URL изображений Unsplash
    val foodImagesMap = mapOf(
        // Завтраки
        "омлет" to "https://images.unsplash.com/photo-1551782450-17144efb9c50",
        "яичница" to "https://images.unsplash.com/photo-1551782450-17144efb9c50",
        "блины" to "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38",
        "каша" to "https://images.unsplash.com/photo-1505253668822-42074d58a7c6",
        "тост" to "https://images.unsplash.com/photo-1483695028939-5bb13f8648b0",

        // Основные блюда
        "паста" to "https://images.unsplash.com/photo-1563379926898-05f4575a45d8",
        "спагетти" to "https://images.unsplash.com/photo-1563379926898-05f4575a45d8",
        "пицца" to "https://images.unsplash.com/photo-1513104890138-7c749659a591",
        "бургер" to "https://images.unsplash.com/photo-1568901346375-23c9450c58cd",
        "стейк" to "https://images.unsplash.com/photo-1600891964092-4316c288032e",
        "курица" to "https://images.unsplash.com/photo-1532550907401-a500c9a57435",
        "рыба" to "https://images.unsplash.com/photo-1467003909585-2f8a72700288",

        // Супы
        "суп" to "https://images.unsplash.com/photo-1547592166-23ac45744acd",
        "борщ" to "https://images.unsplash.com/photo-1547592166-23ac45744acd",
        "щи" to "https://images.unsplash.com/photo-1547592166-23ac45744acd",

        // Салаты
        "салат" to "https://images.unsplash.com/photo-1512621776951-a57141f2eefd",
        "цезарь" to "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe",

        // Десерты
        "десерт" to "https://images.unsplash.com/photo-1563729784474-d77dbb933a9e",
        "торт" to "https://images.unsplash.com/photo-1578985545062-69928b1d9587",
        "печенье" to "https://images.unsplash.com/photo-1558961363-fa8fdf82db35",
        "пирог" to "https://images.unsplash.com/photo-1565958011703-44f9829ba187",

        // Напитки
        "напиток" to "https://images.unsplash.com/photo-1551024709-8f23befc6f87",
        "сок" to "https://images.unsplash.com/photo-1551024709-8f23befc6f87",
        "чай" to "https://images.unsplash.com/photo-1561047029-3000c68339ca",
        "кофе" to "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085",
    )

    // Ищем подходящее изображение
    return foodImagesMap.entries.firstOrNull { (tag, _) ->
        mainTag.contains(tag, ignoreCase = true)
    }?.value ?: getDefaultFoodImage()
}

fun getDefaultFoodImage(): String {
    return "https://images.unsplash.com/photo-1504674900247-0877df9cc836"
}

// Функция для форматирования URL
fun formatUnsplashUrl(baseUrl: String, width: Int = 800, height: Int = 600): String {
    return "$baseUrl?w=$width&h=$height&fit=crop&auto=format"
}