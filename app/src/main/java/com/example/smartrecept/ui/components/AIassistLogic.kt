package com.example.smartrecept.ui.components

import AIRecipe
import RecipeViewModelFactory
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val prompt_preparing_visual = "Ввыведи инофрмацию в формате JSON !!!ТОЛЬКО JSON!!! по следующему алгоритму:\n\n" +
            "в блоке 'ingredients' перечисли ингредиенты в виде массива (списка) [ingr1, ing2, ingr3]\n\n" +
            " блоке 'tags' перечисли теги в виде массива (списка) [tag1, tag2, tag3] которые будут описывать рецепт в одно слово\n\n" +
            "в блоке 'time' укажи примерное время приготовления в минутах (тип String)\n\n" +
            "в блоке 'servings' укажи количество порций (тип Int)\n\n" +
            "в блоке 'steps' напиши шаги приготовления рецепта подробно\n\n" +
            "если требуется, то дабавь в блоке 'notes' заметки или подсказки\n\n"

    var prompt by remember { mutableStateOf("Напиши рецепт омлета") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedModel by remember { mutableStateOf(AiModel.GROQ) }


    Column(
        modifier = Modifier
            .fillMaxSize()
//            .verticalScroll(rememberScrollState())
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        response = "Спрашиваю..."
                        val result = askAI(prompt + prompt_preparing_visual, selectedModel)
                        response = result
//                        println(result)

                    } catch (e: Exception) {
                        response = "ОШИБКА: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
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
                selected = selectedModel == AiModel.DEEPSEEK,
                onClick = { selectedModel = AiModel.DEEPSEEK },
                label = { Text("DeepSeek") }
            )
            FilterChip(
                selected = selectedModel == AiModel.GROQ,
                onClick = { selectedModel = AiModel.GROQ },
                label = { Text("Groq") }
            )
            FilterChip(
                selected = selectedModel == AiModel.TESTER,
                onClick = { selectedModel = AiModel.TESTER },
                label = { Text("Tester") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        PreviewRecipe(response)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewRecipe(jsonRecipe: String) {
    println("=== ПРЕВЬЮ РЕЦЕПТА ===")
    println(jsonRecipe)
    println("======================")

    // Используем safe парсинг на случай ошибок
    val recipe = remember {
        try {
            val parsed = AIJsonParser.parseAIRecipe(jsonRecipe)
            println("✅ Успешно распарсено: ${parsed.airecipe_name}")
            println("Ингредиентов: ${parsed.ingredients.size}")
            println("Шагов: ${parsed.steps.size}")
            parsed
        } catch (e: Exception) {
            // Если ошибка парсинга, создаем пустой рецепт
            AIRecipe(
                airecipe_name = "Ошибка загрузки",
                ingredients = emptyList(),
                tags = emptyList(),
                time = "0",
                servings = 1,
                steps = emptyList(),
                notes = emptyList()
            )
        }
    }

    var servingCoefficient by remember { mutableFloatStateOf(1f) }
    var selectedServings by remember { mutableIntStateOf(recipe.servings) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Шапка с названием и информацией
        item {
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
                        val expanded = remember { mutableStateOf(false) }

                        Box {
                            AssistChip(
                                onClick = { expanded.value = true },
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
                                expanded = expanded.value,
                                onDismissRequest = { expanded.value = false },
                                modifier = Modifier.width(140.dp)
                            ) {
                                // Варианты порций
                                val portionOptions = getPortionOptions(selectedServings)

                                portionOptions.forEach { option ->
                                    DropdownMenuItem(
                                        onClick = {
                                            servingCoefficient = option.toFloat() / recipe.servings.toFloat()
                                            selectedServings = option
                                            expanded.value = false
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
        }

        // 2. Заметки (если есть) - ИСПРАВЛЕННЫЙ ВАРИАНТ
        if (recipe.notes.isNotEmpty()) {
            item {
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
        }

        // 3. Ингредиенты
        item {
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
        }

        // 4. Шаги приготовления - ИСПРАВЛЕННЫЙ ВАРИАНТ
        itemsIndexed(recipe.steps) { index, step ->
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

                    // Текст шага - БЕЗ ограничений по высоте
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    println(step)
                }
            }
        }

        // 5. Пустое пространство внизу
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
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

// Если нужно, добавьте эту функцию в тот же файл или там где есть scaleIngredient
fun scaleIngredient(ingredient: String, coefficient: Float): String {
    // Простая логика масштабирования
    // Можно улучшить для парсинга чисел в ингредиентах
    return try {
        // Пытаемся найти числа в строке
        val regex = """(\d+(?:[.,]\d+)?)""".toRegex()
        var result = ingredient
        regex.findAll(ingredient).forEach { match ->
            val number = match.value.replace(",", ".").toFloat()
            val scaled = number * coefficient
            val formatted = if (scaled % 1 == 0f) {
                scaled.toInt().toString()
            } else {
                String.format("%.1f", scaled).replace(".0", "")
            }
            result = result.replace(match.value, formatted)
        }
        result
    } catch (e: Exception) {
        ingredient
    }
}

// Функция для получения вариантов порций (если нет в вашем коде)
fun getPortionOptions(current: Int): List<Int> {
    return when {
        current <= 2 -> listOf(1, 2, 3, 4)
        current <= 4 -> listOf(1, 2, 4, 6)
        else -> listOf(1, 2, 4, 6, 8)
    }
}

enum class AiModel {
    GEMINI, DEEPSEEK, GROQ, TESTER
}

suspend fun askAI(prompt: String, model: AiModel = AiModel.GEMINI): String {
    return when (model) {
        AiModel.GEMINI -> askGemini(prompt)
        AiModel.DEEPSEEK -> askTester(prompt)
        AiModel.GROQ -> askTester(prompt)
        AiModel.TESTER -> askTester(prompt)
    }
}

@OptIn(PublicPreviewAPI::class)
suspend fun askGemini(prompt: String): String {
    return try {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")

        val result = model.generateContent(prompt)
        result.text ?: "Gemini не ответил"

    } catch (e: Exception) {
        "ОШИБКА: ${e.message}\n\n"
    }
}

suspend fun askTester(prompt: String): String {
    return """
    {
      "airecipe_name": "Тестовый рецепт: 1",
      "ingredients": ["3 яйца", "100 мл молока", "соль", "перец", "масло"],
      "tags": ["тест", "быстро", "просто"],
      "time": "15",
      "servings": 2,
      "steps": [
        "Подготовьте все ингредиенты",
        "Смешайте яйца с молоком",
        "Добавьте соль и перец",
        "Готовьте на среднем огне 10 минут"
      ],
      "notes": ["Тестовый рецепт для разработки", "Замените на реальный API"]
    }
    """.trimIndent()
}
//
//suspend fun askGroq(prompt: String): String {
//    return withContext(Dispatchers.IO) {
//        try {
//            val apiKey = ""
//
//            // Если ключ пустой или не начинается с gsk_
//            if (apiKey.isEmpty() || apiKey == "\"\"" || !apiKey.startsWith("gsk_")) {
//                return@withContext "❌ ОШИБКА: Неверный формат ключа Groq.\n" +
//                        "Ожидается: 'gsk_********'\n" +
//                        "Получено: '${apiKey.take(20)}'\n\n" +
//                        "Проверьте local.properties и синхронизацию Gradle."
//            }
//
//            // Настраиваем клиент
//            val client = OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .addInterceptor { chain ->
//                    val request = chain.request().newBuilder()
//                        .addHeader("User-Agent", "SmartRecept/1.0")
//                        .build()
//                    chain.proceed(request)
//                }
//                .build()
//
//            // Доступные модели Groq (бесплатные):
//            // - "llama3-8b-8192"           // Быстрая, хорошая
//            // - "llama3.1-8b-instant"      // Очень быстрая
//            // - "mixtral-8x7b-32768"       // Качественная, но медленнее
//            // - "gemma2-9b-it"             // Google модель
//            // - "qwen-2.5-32b"             // Алибаба модель
//
//            val model = "openai/gpt-oss-120b" // Рекомендую эту для начала
//
//            // Формируем запрос
//            val jsonBody = JSONObject().apply {
//                put("model", model)
//                put("messages", JSONArray().apply {
//                    put(JSONObject().apply {
//                        put("role", "user")
//                        put("content", prompt)
//                    })
//                })
//                put("temperature", 0.7)
//                put("max_tokens", 4000)
////                put("top_p", 0.9)
////                put("stream", false)
//            }
//
//            println("Отправляю запрос к Groq (модель: $model)...")
//
//            val request = Request.Builder()
//                .url("https://api.groq.com/openai/v1/chat/completions")
//                .addHeader("Authorization", "Bearer $apiKey")
//                .addHeader("Content-Type", "application/json")
//                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
//                .build()
//
//            val response = client.newCall(request).execute()
//            val responseBody = response.body?.string()
//
//            println("Ответ от Groq: код ${response.code}")
//
//            // Обрабатываем ответ
//            when {
//                !response.isSuccessful -> {
//                    val errorMsg = when (response.code) {
//                        401 -> "❌ Неверный API ключ Groq"
//                        429 -> "❌ Превышен лимит запросов (30/мин). Попробуйте через минуту."
//                        402 -> "❌ Требуется оплата для Groq" // На всякий случай
//                        else -> "❌ Ошибка ${response.code}: ${response.message}"
//                    }
//                    "$errorMsg\n${responseBody?.take(200) ?: ""}"
//                }
//
//                responseBody == null -> {
//                    "❌ Пустой ответ от Groq"
//                }
//
//                else -> {
//                    try {
//                        val json = JSONObject(responseBody)
//
//                        // Проверяем структуру ответа
//                        if (!json.has("choices") || json.getJSONArray("choices").length() == 0) {
//                            // Иногда Groq возвращает другой формат
//                            if (json.has("generated_text")) {
//                                return@withContext json.getString("generated_text")
//                            }
//                            return@withContext "❌ Неверный формат ответа от Groq"
//                        }
//
//                        val choices = json.getJSONArray("choices")
//                        val firstChoice = choices.getJSONObject(0)
//
//                        // OpenAI-совместимый формат
//                        if (firstChoice.has("message")) {
//                            val message = firstChoice.getJSONObject("message")
//                            val content = message.getString("content")
//
//                            // Очищаем ответ
//                            val cleaned = content
//                                .replace("```json", "")
//                                .replace("```", "")
//                                .trim()
//
//                            println("Успешный ответ от Groq (${cleaned.length} символов)")
//
//                            // Добавляем информацию об использовании токенов
//                            if (json.has("usage")) {
//                                val usage = json.getJSONObject("usage")
//                                val totalTokens = usage.getInt("total_tokens")
//                                println("Использовано токенов: $totalTokens")
//                            }
//
//                            return@withContext cleaned
//                        } else {
//                            // Альтернативный формат
//                            return@withContext firstChoice.getString("text")
//                        }
//
//                    } catch (e: Exception) {
//                        println("Ошибка парсинга JSON: ${e.message}")
//                        "❌ Ошибка парсинга ответа Groq: ${e.message}\n\nОтвет: ${responseBody?.take(500)}"
//                    }
//                }
//            }
//
//        } catch (e: java.net.SocketTimeoutException) {
//            "❌ Таймаут подключения к Groq. Модель может быть перегружена."
//        } catch (e: java.net.UnknownHostException) {
//            "❌ Не удается подключиться к Groq. Проверьте интернет."
//        } catch (e: Exception) {
//            "❌ Ошибка Groq: ${e.message ?: "Неизвестная ошибка"}"
//        }
//    }
//}
//
//suspend fun askDeepSeek(prompt: String): String {
//    return withContext(Dispatchers.IO) {
//        try {
//            // 1. Получаем API ключ из BuildConfig
//            val apiKey = BuildConfig.DEEPSEEK_API_KEY
//
//            if (apiKey.isEmpty() || apiKey == "\"\"" || apiKey.contains("ваш")) {
//                return@withContext "❌ ОШИБКА: API ключ DeepSeek не настроен.\n" +
//                        "Добавьте ключ в local.properties: deepseek.api.key=ваш_ключ\n" +
//                        "И синхронизируйте Gradle (File → Sync Project with Gradle Files)"
//            }
//
//            println("Используем DeepSeek API ключ: ${apiKey.take(10)}...")
//
//            // 2. Настраиваем HTTP клиент
//            val client = OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .build()
//
//            // 3. Формируем запрос в формате JSON
//            val messages = JSONArray().apply {
//                put(JSONObject().apply {
//                    put("role", "user")
//                    put("content", prompt)
//                })
//            }
//
//            val jsonBody = JSONObject().apply {
//                put("model", "deepseek-chat")
//                put("messages", messages)
//                put("stream", false)
//                put("max_tokens", 4000)
//                put("temperature", 0.7)
//            }
//
//            println("Отправляю запрос к DeepSeek...")
//            println("Длина промпта: ${prompt.length} символов")
//
//            // 4. Создаем HTTP запрос
//            val request = Request.Builder()
//                .url("https://api.deepseek.com/chat/completions")
//                .addHeader("Authorization", "Bearer $apiKey")
//                .addHeader("Content-Type", "application/json")
//                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
//                .build()
//
//            // 5. Выполняем запрос
//            val response = client.newCall(request).execute()
//            val responseBody = response.body?.string()
//
//            println("Ответ от DeepSeek: код ${response.code}")
//
//            // 6. Обрабатываем ответ
//            when {
//                !response.isSuccessful -> {
//                    val error = when (response.code) {
//                        401 -> "❌ Неверный API ключ DeepSeek"
//                        429 -> "❌ Превышен лимит запросов. Попробуйте позже."
//                        500 -> "❌ Ошибка сервера DeepSeek"
//                        else -> "❌ Ошибка ${response.code}: ${response.message}"
//                    }
//                    "$error\nТело ответа: ${responseBody?.take(200)}"
//                }
//
//                responseBody == null -> {
//                    "❌ Пустой ответ от DeepSeek"
//                }
//
//                else -> {
//                    try {
//                        // 7. Парсим JSON ответ
//                        val json = JSONObject(responseBody)
//
//                        if (!json.has("choices")) {
//                            return@withContext "❌ Неверный формат ответа от DeepSeek"
//                        }
//
//                        val choices = json.getJSONArray("choices")
//                        if (choices.length() == 0) {
//                            return@withContext "❌ Пустой ответ от DeepSeek"
//                        }
//
//                        val firstChoice = choices.getJSONObject(0)
//                        val message = firstChoice.getJSONObject("message")
//                        val content = message.getString("content")
//
//                        // 8. Очищаем ответ (иногда AI добавляет лишний текст)
//                        val cleanedContent = content
//                            .replace("```json", "")
//                            .replace("```", "")
//                            .trim()
//
//                        println("Успешно получен ответ от DeepSeek (${cleanedContent.length} символов)")
//
//                        // 9. Добавляем информацию о токенах (опционально)
//                        if (json.has("usage")) {
//                            val usage = json.getJSONObject("usage")
//                            val totalTokens = usage.getInt("totalTokens")
//                            println("Использовано токенов: $totalTokens")
//                        }
//
//                        cleanedContent
//
//                    } catch (e: Exception) {
//                        "❌ Ошибка парсинга JSON от DeepSeek: ${e.message}\n\nОтвет: ${responseBody.take(500)}"
//                    }
//                }
//            }
//
//        } catch (e: java.net.SocketTimeoutException) {
//            "❌ Таймаут подключения к DeepSeek. Проверьте интернет."
//        } catch (e: java.net.UnknownHostException) {
//            "❌ Не удается подключиться к DeepSeek. Проверьте интернет."
//        } catch (e: Exception) {
//            "❌ Ошибка DeepSeek: ${e.message ?: "Неизвестная ошибка"}\n${e.stackTraceToString().take(200)}"
//        }
//    }
//}