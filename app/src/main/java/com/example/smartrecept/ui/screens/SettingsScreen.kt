// ui/screens/SettingsScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.smartrecept.data.settings.UserPreferencesRepository
import com.example.smartrecept.data.settings.UserPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartrecept.data.recipes.DatasourceRecipes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
    var showClearFavouriteDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearFavouriteDialog) {
        AlertDialog(
            onDismissRequest = { showClearFavouriteDialog = false },
            title = { Text("Очистить избранное") },
            text = { Text("Вы уверены, что хотите удалить все рецепты из избранного?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFavorites()
                        showClearFavouriteDialog = false
                    }
                ) {
                    Text("Очистить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFavouriteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Очистить базу данных") },
            text = { Text("Вы уверены, что хотите стереть все данные?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        Log.d("DELETE", "DELETE")
                        showClearAllDialog = false
                    }
                ) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        // Выбор темы оформление
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

        // Выбор масштаба / размера шрифта
        var fontSize by remember { mutableStateOf(FontSizeOption.M) }

        FontSizeSelector(
            selectedSize = fontSize,
            onSizeSelected = {
                scope.launch { repository.updateFontScale(it.size.toFloat()) }
                fontSize = it
            }
        )

        Divider()

        // Выбор языка
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Язык", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                scope.launch {
                    val newLang = if (preferences.language == "en") "ru" else "en"
                    repository.updateLanguage(newLang)
                }
            }) {
                Text(text = preferences.language.uppercase())
            }
        }

        Divider()

        // Работа с бд
        DatabaseManagementPanel(
            onDeleteFavorites = {
                // Логика удаления избранного
                showClearFavouriteDialog = true
            },
            onDeleteAllRecipes = {
                // Логика удаления всех рецептов
                showClearAllDialog = true
            },
            onExportData = {
                // Логика экспорта данных
            },
            onImportJSON = { jsonContent ->
                // Логика импорта JSON
                ImportResult(success = true, message = "Данные импортированы", importedCount = 2)
            },
            onImportCSV = { csvContent ->
                // Логика импорта CSV
                ImportResult(success = true, message = "Данные импортированы", importedCount = 2)
            }
        )
    }
}

enum class FontSizeOption(val label: String, val size: Int) {
    XS("XS", 12),
    S("S", 14),
    M("M", 16),
    L("L", 18),
    XL("XL", 20)
}

@Composable
fun FontSizeSelector(
    selectedSize: FontSizeOption = FontSizeOption.M,
    onSizeSelected: (FontSizeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.LightGray.copy(alpha = 0.2f))
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FontSizeOption.entries.forEach { option ->
            FontSizeButton(
                option = option,
                isSelected = option == selectedSize,
                onClick = { onSizeSelected(option) }
            )
        }
    }
}

@Composable
fun FontSizeButton(
    option: FontSizeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            fontSize = option.size.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// Модели для демонстрационных данных
data class DemoRecipe(
    val id: Int,
    val title: String,
    val category: String,
    val time: Int,
    val rating: Float
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val importedCount: Int = 0
)

@Composable
fun DatabaseManagementPanel(
    onDeleteFavorites: () -> Unit,
    onDeleteAllRecipes: () -> Unit,
    onExportData: () -> Unit,
    onImportJSON: (String) -> ImportResult,
    onImportCSV: (String) -> ImportResult,
    modifier: Modifier = Modifier
) {
    var showDemoTemplate by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importType by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = "Управление базой данных",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Основные операции
        DatabaseOperationCard(
            title = "Очистка данных",
            description = "Управление содержимым базы данных",
            operations = listOf(
                DatabaseOperation(
                    title = "Удалить избранное",
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFFF6B6B),
                    onClick = onDeleteFavorites
                ),
                DatabaseOperation(
                    title = "Удалить все рецепты",
                    icon = Icons.Default.DeleteForever,
                    color = Color(0xFFE74C3C),
                    onClick = onDeleteAllRecipes
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Импорт/Экспорт
        DatabaseOperationCard(
            title = "Импорт/Экспорт",
            description = "Работа с внешними данными",
            operations = listOf(
                DatabaseOperation(
                    title = "Экспорт данных",
                    icon = Icons.Default.Download,
                    color = Color(0xFF3498DB),
                    onClick = onExportData
                ),
                DatabaseOperation(
                    title = "Импорт данных",
                    icon = Icons.Default.Upload,
                    color = Color(0xFF2ECC71),
                    onClick = {
                        showImportDialog = true
                    }
                ),
                DatabaseOperation(
                    title = "Шаблоны",
                    icon = Icons.Default.Info,
                    color = Color(0xFF9B59B6),
                    onClick = { showDemoTemplate = true }
                )
            )
        )

        // Диалог импорта
        if (showImportDialog) {
            ImportFileDialog(
                onDismiss = { showImportDialog = false },
                onImportJSON = { jsonContent ->
                    val result = onImportJSON(jsonContent)
                    importResult = result
                    importType = "JSON"
                },
                onImportCSV = { csvContent ->
                    val result = onImportCSV(csvContent)
                    importResult = result
                    importType = "CSV"
                }
            )
        }

        // Результат импорта
        importResult?.let { result ->
            ImportResultDialog(
                result = result,
                importType = importType,
                onDismiss = { importResult = null }
            )
        }

        // Демонстрационный шаблон
        if (showDemoTemplate) {
            DemoTemplateDialog(
                onDismiss = { showDemoTemplate = false }
            )
        }
    }
}

@Composable
fun DatabaseOperationCard(
    title: String,
    description: String,
    operations: List<DatabaseOperation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            operations.forEach { operation ->
                DatabaseOperationButton(
                    operation = operation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

data class DatabaseOperation(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun DatabaseOperationButton(
    operation: DatabaseOperation,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = operation.onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = operation.color.copy(alpha = 0.9f),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = operation.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(operation.title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFileDialog(
    onDismiss: () -> Unit,
    onImportJSON: (String) -> Unit,
    onImportCSV: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var jsonContent by remember { mutableStateOf("") }
    var csvContent by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Импорт данных",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // JSON импорт
                Text(
                    text = "JSON данные:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = jsonContent,
                    onValueChange = { jsonContent = it },
                    placeholder = { Text("Вставьте JSON данные...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false
                )
                Button(
                    onClick = { onImportJSON(jsonContent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = jsonContent.isNotBlank()
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Импорт JSON")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CSV импорт
                Text(
                    text = "CSV данные:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = csvContent,
                    onValueChange = { csvContent = it },
                    placeholder = { Text("Вставьте CSV данные...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false
                )
                Button(
                    onClick = { onImportCSV(csvContent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = csvContent.isNotBlank()
                ) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Импорт CSV")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun ImportResultDialog(
    result: ImportResult,
    importType: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (result.success) "Импорт успешен" else "Ошибка импорта")
        },
        text = {
            Text("$importType импорт: ${result.message}\nИмпортировано: ${result.importedCount}")
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoTemplateDialog(
    onDismiss: () -> Unit
) {
    val jsonTemplate = """
        {
          "recipes": [
            {
              "id": 1,
              "title": "Паста Карбонара",
              "tags": ["Итальянская", "быстрое"],
              "time": 20,
              "servings": 1,
              "image": "http://example.com/photo1.jpg",
              "isFavorite": false,
              "ingredients": ["спагетти", "яйца", "пармезан", "бекон"],
              "steps": ["Варить пасту", "Обжарить бекон", "Смешать с соусом"],
              "notes": []
            },
            {
              "id": 2,
              "title": "Салат Цезарь",
              "tags": ["Салаты", "быстрое"],
              "time": 15,
              "servings": 2,
              "image": "http://example.com/photo2.jpg",
              "isFavorite": false,
              "ingredients": ["салат", "курица", "сухарики", "соус"],
              "steps": ["Подготовить ингредиенты", "Смешать", "Заправить"],
              "notes": []
            }
          ]
        }
    """.trimIndent()

    val csvTemplate = """
        title,tags,time,servings,isFavorite,ingredients,steps
        "Паста Карбонара","Итальянская;быстрое",20,2,"http://example.com/photo1.jpg",false,"спагетти;яйца;пармезан;бекон","Варить пасту;Обжарить бекон;Смешать с соусом"
        "Салат Цезарь","Салаты;быстрое",15,1,"http://example.com/photo2.jpg",false,"салат;курица;сухарики;соус","Подготовить ингредиенты;Смешать;Заправить"
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Шаблоны для импорта",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "JSON формат:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = jsonTemplate,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Text(
                    text = "CSV формат:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(75.dp)
                        .horizontalScroll(rememberScrollState()),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = csvTemplate,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Понятно")
                }
            }
        }
    }
}