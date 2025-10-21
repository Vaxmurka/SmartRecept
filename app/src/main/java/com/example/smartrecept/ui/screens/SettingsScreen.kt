// ui/screens/SettingsScreen.kt
package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.app.Application
import android.content.Intent
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartrecept.data.recipes.DatasourceRecipes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import java.io.File
import java.io.FileOutputStream

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
    val context = LocalContext.current
    val preferences by repository.preferencesFlow.collectAsState(initial = UserPreferences())
    var showClearFavouriteDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Состояние экспорта
    val exportState by viewModel.exportState.collectAsState()

    // Функция для сохранения файла и запуска шеринга
    val saveAndShareFile: (String, String, String) -> Unit = { content, fileName, type ->
        try {
            // Создаем временный файл в кэше приложения
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }

            // Создаем URI через FileProvider для безопасного доступа
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            // Создаем Intent для шеринга/скачивания
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                when (type) {
                    "JSON" -> setType("application/json")
                    "CSV" -> setType("text/csv")
                    else -> setType("text/plain")
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Экспорт рецептов")
                putExtra(Intent.EXTRA_TITLE, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Запускаем диалог выбора приложения для сохранения/шеринга
            context.startActivity(Intent.createChooser(shareIntent, "Сохранить файл"))

            Toast.makeText(context, "Файл готов к сохранению: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при экспорте: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Обработка состояний экспорта
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is RecipeViewModel.ExportState.Ready -> {
                // Сохраняем файл и запускаем шеринг
                saveAndShareFile(state.content, state.fileName, state.type)
                viewModel.resetExportState()
            }
            is RecipeViewModel.ExportState.Error -> {
                // Показываем ошибку
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    val fontType = preferences.font

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

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Выберите формат экспорта") },
            text = { Text("В каком формате вы хотите экспортировать данные?") },
            confirmButton = {
                Column {
                    Button(
                        onClick = {
                            viewModel.prepareJsonExport()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("JSON")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.prepareCsvExport()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CSV")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Кнопка Отмена
                    OutlinedButton(
                        onClick = { showExportDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отмена")
                    }
                }
            },
            dismissButton = null
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

        val selectedTheme = preferences.themeMode
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

        Text("Масштаб", style = MaterialTheme.typography.titleMedium)
        FontSizeSelector(
            selectedSize = fontSize,
            onSizeSelected = {
                scope.launch { repository.updateFont(it) }
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
                showClearFavouriteDialog = true
            },
            onDeleteAllRecipes = {
                showClearAllDialog = true
            },
            onExportData = {
                showExportDialog = true
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
    val message: String
)

@Composable
fun DatabaseManagementPanel(
    onDeleteFavorites: () -> Unit,
    onDeleteAllRecipes: () -> Unit,
    onExportData: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)),
) {
    var showDemoTemplate by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importType by remember { mutableStateOf("") }

    var showResultDialog by remember { mutableStateOf(false) }
    var importResultMessage by remember { mutableStateOf("") }

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
                    importType = "JSON"
                    showImportDialog = false
                    viewModel.importFromJson(jsonContent) { success, message ->
                        importResult = ImportResult(success, message)
                    }
                },
                onImportCSV = { csvContent ->
                    viewModel.importFromCsv(csvContent) { success, message ->
                        importResult = ImportResult(success, message)
                    }
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
    val icon: ImageVector,
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
    // Эти состояния для ручного ввода можно оставить
    var jsonContent by remember { mutableStateOf("") }
    var csvContent by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                // Получаем имя файла из метаданных Uri
                var fileName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }

                // Читаем содержимое файла
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val fileContent = stream.bufferedReader().readText()

                    // Теперь проверяем имя файла, которое мы получили
                    if (fileName?.endsWith(".json", ignoreCase = true) == true) {
                        onImportJSON(fileContent)
                    } else if (fileName?.endsWith(".csv", ignoreCase = true) == true) {
                        onImportCSV(fileContent)
                    } else {
                        // Эта ошибка теперь будет показываться только если расширение действительно другое
                        Toast.makeText(context, "Неподдерживаемый формат файла. Выбран файл: $fileName", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                // Обрабатываем возможные ошибки чтения файла
                Toast.makeText(context, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Центрируем для красоты
            ) {
                Text(
                    text = "Импорт данных",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Упрощаем UI: оставляем только одну кнопку для выбора файла
                Text(
                    text = "Выберите файл .json или .csv для импорта ваших рецептов.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        // Запускаем выбор файлов с нужными MIME-типами
//                        importLauncher.launch(arrayOf("application/json", "text/csv"))
                        importLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать файл для импорта")
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
            Text("$importType импорт: \n${result.message}")
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
        [
          {
            "id": 1,
            "title": "Паста Карбонара",
            "tags": ["Итальянская", "быстрое"],
            "time": "20 мин",
            "image": "",
            "servings": 2,
            "isFavorite": false,
            "isCooked": false,
            "ingredients": ["спагетти - 200г", "яйца - 2 шт", "пармезан - 50г", "бекон - 100г"],
            "steps": ["Варить пасту 10 минут", "Обжарить бекон", "Смешать с соусом"],
            "notes": [],
            "stepImages": []
          },
          {
            "id": 2,
            "title": "Салат Цезарь",
            "tags": ["Салаты", "быстрое"],
            "time": "15 мин",
            "image": "",
            "servings": 2,
            "isFavorite": false,
            "isCooked": false,
            "ingredients": ["салат айсберг - 1 шт", "курица - 200г", "сухарики - 50г", "соус цезарь - 2 ст.л."],
            "steps": ["Подготовить ингредиенты", "Нарезать салат и курицу", "Смешать все ингредиенты", "Заправить соусом"],
            "notes": [],
            "stepImages": []
          }
        ]
    """.trimIndent()

    val csvTemplate = """
        ID,Title,Tags,Time,Image,Servings,IsFavorite,IsCooked,Ingredients,Steps,Notes,StepImages
        1,"Паста Карбонара","Итальянская;быстрое","20 мин","",2,false,false,"спагетти - 200г;яйца - 2 шт;пармезан - 50г;бекон - 100г","Варить пасту 10 минут;Обжарить бекон;Смешать с соусом","",""
        2,"Салат Цезарь","Салаты;быстрое","15 мин","",2,false,false,"салат айсберг - 1 шт;курица - 200г;сухарики - 50г;соус цезарь - 2 ст.л.","Подготовить ингредиенты;Нарезать салат и курицу;Смешать все ингредиенты;Заправить соусом","",""
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
                        .height(150.dp)
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