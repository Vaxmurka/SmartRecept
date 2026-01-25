package com.example.smartrecept.data.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // 1. Создаем экземпляр модели Gemini, используя ваш ключ
    private val generativeModel = GenerativeModel(
        // Используйте "gemini-1.5-flash" для скорости и чата, или "gemini-pro"
        modelName = "gemini-1.5-flash",
        // Вот так безопасно используется ваш ключ из build.gradle.kts
//        apiKey = BuildConfig.GEMINI_API_KEY
        apiKey = "AIzaSyB2r25tZkJXs1k2hEnoMetPHW_jBoZdQY4"
    )

    // StateFlow для хранения ответа от Gemini, чтобы UI мог на него подписаться
    private val _responseText = MutableStateFlow("Готов к работе. Задайте ваш вопрос.")
    val responseText: StateFlow<String> = _responseText.asStateFlow()

    // StateFlow для отслеживания состояния загрузки (чтобы показывать/скрывать ProgressBar)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 2. Функция для отправки запроса
    fun sendPrompt(prompt: String) {
        // Если уже идет загрузка, ничего не делаем
        if (_isLoading.value) {
            return
        }
        // Устанавливаем флаг загрузки
        _isLoading.value = true
        _responseText.value = "Думаю..."

        // Запускаем корутину в viewModelScope, она автоматически отменится, если ViewModel уничтожится
        viewModelScope.launch {
            try {
                // Отправляем запрос модели
                val response = generativeModel.generateContent(prompt)

                // Обновляем StateFlow с ответом
                _responseText.value = response.text ?: "Ошибка: получен пустой ответ."
            } catch (e: Exception) {
                // Обрабатываем возможные ошибки (нет интернета, неверный ключ и т.д.)
                _responseText.value = "Произошла ошибка: ${e.message}"
            } finally {
                // В любом случае (успех или ошибка) снимаем флаг загрузки
                _isLoading.value = false
            }
        }
    }
}