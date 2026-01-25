// GeminiTestScreen.kt
package com.example.smartrecept.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.launch

@OptIn(PublicPreviewAPI::class)
@Composable
fun GeminiTestScreen(
    navController: NavController
) {
    var prompt by remember { mutableStateOf("Напиши рецепт омлета") }
    var response by remember { mutableStateOf("Нажми кнопку чтобы спросить у Gemini") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val prompt_preparing_visual = "Ввыведи инофрмацию в формате JSON по следующему алгоритму:\n\n" +
            "в блоке 'ingredients' перечисли ингредиенты в виде массива (списка) [ingr1, ing2, ingr3]\n\n" +
            " блоке 'tags' перечисли теги в виде массива (списка) [tag1, tag2, tag3] которые будут описывать рецепт в одно слово\n\n" +
            "в блоке 'time' укажи примерное время приготовления в минутах\n\n" +
            "в блоке 'servings' укажи количество порций\n\n" +
            "в блоке 'steps' напиши шаги приготовления рецепта подробно\n\n" +
            "если требуется, то дабавь в блоке 'notes' заметки или подсказки\n\n"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                        val result = askGemini(prompt + prompt_preparing_visual)
                        response = result

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
            } else {
                Text("Спросить у Gemini")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = response,
            modifier = Modifier.fillMaxWidth()
        )

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