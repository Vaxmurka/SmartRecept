// ui/screens/SettingsScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.data.settings.UserPreferences

@Composable
fun SettingsScreen(
    repository: UserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    SettingsApp(repository, modifier)
}

@Composable
fun SettingsApp(
    repository: UserPreferencesRepository,
    modifier: Modifier = Modifier,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить избранное") },
            text = { Text("Вы уверены, что хотите удалить все рецепты из избранного?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFavorites()
                        showClearDialog = false
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        // Theme Block
        Text("Тема", style = MaterialTheme.typography.titleMedium)

        val selectedTheme = preferences.themeMode // Assume: "light", "dark", "system"
        val themeOptions = listOf("Light", "Dark", "System")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            themeOptions.forEach { option ->
                val isSelected = selectedTheme == option.lowercase()
                Button(
                    onClick = {
                        scope.launch { repository.updateThemeMode(option.lowercase()) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(option)
                }
            }
        }

        Divider()

        // Font size
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Размер шрифта", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = preferences.fontScale,
                onValueChange = { scope.launch { repository.updateFontScale(it) } },
                valueRange = 0.8f..1.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider()

        // Language
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Язык", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                scope.launch {
                    val newLang = if (preferences.language == "en") "ru" else "enz"
                    repository.updateLanguage(newLang)
                }
            }) {
                Text(text = preferences.language.uppercase())
            }
        }

        Divider()

        // Clear Favorites
        Button(
            onClick = {
                showClearDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Очистить избранное", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    MaterialTheme {
        val context = LocalContext.current
        val userPrefsRepo = remember { UserPreferencesRepository(context) }
        SettingsApp(userPrefsRepo, Modifier)
    }
}
