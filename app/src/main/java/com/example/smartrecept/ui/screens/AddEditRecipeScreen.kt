package com.example.smartrecept.ui.screens

import RecipeViewModelFactory
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.smartrecept.data.recipes.RecipeStep
import com.example.smartrecept.filterChipsList
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

// ----------------------------
// Утилиты для сохранения фото
// ----------------------------
fun createImageFile(context: Context, prefix: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${prefix}_photo_${timeStamp}.jpg"
    return File(context.cacheDir, fileName).apply {
        parentFile?.mkdirs() // Создаем директории если нужно
    }
}

fun Context.copyUriToInternalStorage(uri: Uri, prefix: String, callback: (String?) -> Unit) {
    try {
        val inputStream = contentResolver.openInputStream(uri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${prefix}_photo_${timeStamp}.jpg"
        val file = File(cacheDir, fileName)

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        callback(file.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        callback(null)
    }
}

// ----------------------------
// Основной экран
// ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecipeScreen(
    navController: androidx.navigation.NavHostController,
    recipeId: Int? = null
) {
    var recipeName by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(1) }
    var servingsText by remember { mutableStateOf(servings.toString()) }
    var recipeImage by remember { mutableStateOf<String?>(null) }
    val ingredients = remember { mutableStateListOf("") }
    val steps = remember { mutableStateListOf(RecipeStep()) }
    val notes = remember { mutableStateListOf("") }
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    var isInitialLoad by remember { mutableStateOf(true) }

    val viewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    LaunchedEffect(recipeId) {
        if (recipeId != null && isInitialLoad) {
            viewModel.loadRecipe(recipeId)
            isInitialLoad = false
        }
    }

    fun saveRecipe() {
        viewModel.saveRecipe(
            id = recipeId,
            title = recipeName,
            tags = selectedTags,
            time = cookTime,
            image = recipeImage,
            servings = servings,
            ingredients = ingredients.filter { it.isNotBlank() },
            steps = steps.map { it.text }.filter { it.isNotBlank() },
            stepImages = steps.map { it.imageUri },
            navController = navController,
            notes = notes.filter { it.isNotBlank() },
        )
    }

    val state by viewModel.recipeState.collectAsState()

    if (state is RecipeState.Success && !isInitialLoad) {
        val recipe = (state as RecipeState.Success).recipe
        LaunchedEffect(recipe) {
            if (recipeId != null) {
                recipeName = recipe.title
                cookTime = recipe.time
                servings = recipe.servings
                recipeImage = recipe.image
                ingredients.clear(); ingredients.addAll(recipe.ingredients)
                steps.clear()
                recipe.steps.forEachIndexed { index, stepText ->
                    val stepImage = if (index < recipe.stepImages.size) recipe.stepImages[index] else null
                    steps.add(RecipeStep(stepText, stepImage))
                }
                selectedTags = recipe.tags.toSet()
                notes.clear(); notes.addAll(recipe.notes)
            }
            isInitialLoad = true
        }
    }

    val isFormValid = recipeName.isNotBlank() &&
            cookTime.isNotBlank() &&
            ingredients.all { it.isNotBlank() } &&
            steps.all { it.text.isNotBlank() }

    LaunchedEffect(servings) { servingsText = servings.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (recipeId == null) "Создание рецепта" else "Изменение рецепта")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { saveRecipe() }, enabled = isFormValid) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            RecipeImageBlock(
                imageUri = recipeImage,
                onImageChanged = { recipeImage = it }
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = recipeName,
                onValueChange = { recipeName = it },
                label = { Text("Название рецепта") },
                placeholder = { Text("Паста карбонара") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Время + порции
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = cookTime,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cookTime = it },
                    label = { Text("Время") },
                    trailingIcon = { Text("мин") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                            servingsText = newValue
                            newValue.toIntOrNull()?.let { num -> servings = num.coerceAtLeast(1) }
                        }
                    },
                    label = { Text("Порции") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                servings = max(1, servings - 1)
                                servingsText = servings.toString()
                            }) { Icon(Icons.Default.Remove, null) }
                            IconButton(onClick = {
                                servings++
                                servingsText = servings.toString()
                            }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Ингредиенты", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            ingredients.forEachIndexed { index, ingredient ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ingredient,
                        onValueChange = { ingredients[index] = it },
                        placeholder = { Text("Напишите что-нибудь...") },
                        modifier = Modifier.weight(1f)
                    )
                    if (ingredients.size > 1) {
                        IconButton(onClick = { ingredients.removeAt(index) }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }
            Button(onClick = { ingredients.add("") }) {
                Icon(Icons.Default.Add, null)
                Text("Новый ингредиент")
            }

            Spacer(Modifier.height(24.dp))

            Text("Шаги", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                StepWithImageBlock(
                    step = step,
                    stepNumber = index + 1,
                    onStepTextChanged = { steps[index] = steps[index].copy(text = it) },
                    onImageSelected = { steps[index] = steps[index].copy(imageUri = it) },
                    onRemoveStep = { if (steps.size > 1) steps.removeAt(index) },
                    navController = navController
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(onClick = { steps.add(RecipeStep()) }) {
                Icon(Icons.Default.Add, null)
                Text("Новый шаг")
            }

            Spacer(Modifier.height(24.dp))

            val recipes = viewModel.recipes.collectAsState().value
            val allTags = remember(recipes) {
                // Считаем частоту использования каждого тега
                val tagFrequency = mutableMapOf<String, Int>()

                recipes.forEach { recipe ->
                    recipe.tags
                        .filter { it.isNotBlank() }
                        .forEach { tag ->
                            tagFrequency[tag] = tagFrequency.getOrDefault(tag, 0) + 1
                        }
                }

                // Сортируем по частоте использования (по убыванию)
                val popularTags = tagFrequency
                    .toList()
                    .sortedByDescending { (_, count) -> count }
                    .take(10) // Берем топ-10 самых популярных
                    .map { (tag, _) -> tag }

                // Добавляем стандартные теги, если их нет в популярных
                val defaultTags = filterChipsList
                val combinedTags = (defaultTags + popularTags).distinct()

                // Можем перемешать или выбрать определенное количество
                combinedTags.take(15) // Ограничиваем общее количество
            }

            TagsInputField(
                allTags = allTags,
                selectedTags = selectedTags,
                onTagsChanged = { selectedTags = it }
            )
        }
    }
}

// ----------------------------
// Блок для шага с фото
// ----------------------------
@Composable
fun StepWithImageBlock(
    step: RecipeStep,
    stepNumber: Int,
    onStepTextChanged: (String) -> Unit,
    onImageSelected: (String?) -> Unit,
    onRemoveStep: () -> Unit,
    navController: NavController
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("$stepNumber.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRemoveStep) {
                Icon(Icons.Default.Delete, "Удалить шаг")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = step.text,
            onValueChange = onStepTextChanged,
            placeholder = { Text("Опишите этот шаг приготовления...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(Modifier.height(12.dp))
        StepImageBlock(
            stepIndex = stepNumber - 1,
            imageUri = step.imageUri,
            onImageChanged = onImageSelected
        )
    }
}

// ----------------------------
// Фото для шага
// ----------------------------
@Composable
fun StepImageBlock(
    stepIndex: Int,
    imageUri: String?,
    onImageChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    val photoFile = remember { createImageFile(context, "step_$stepIndex") }
    val photoUri = remember(photoFile) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // --- Камера ---
    val launcherCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageChanged(photoFile.absolutePath) // сохраняем путь
        }
    }

    // --- Галерея ---
    val launcherGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.copyUriToInternalStorage(it, "step_$stepIndex") { savedPath ->
                if (savedPath != null) onImageChanged(savedPath)
            }
        }
    }

    // --- Разрешение на камеру ---
    val cameraPermission = Manifest.permission.CAMERA
    val launcherPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launcherCamera.launch(photoUri)
        } else {
            Toast.makeText(context, "Камера недоступна без разрешения", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Если картинки нет и ссылка не задана → вообще не рисуем блок ---
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .clickable { showSheet = true },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            // Для локального пути безопаснее обернуть в file://
            val model = if (imageUri.startsWith("/")) {
                Uri.fromFile(File(imageUri))
            } else imageUri

            AsyncImage(
                model = model,
                contentDescription = "Recipe Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Text("Добавить фото", color = Color.Gray)
            }
        }
    }

    if (showSheet) {
        ImagePickerBottomSheet(
            onPickCamera = {
                showSheet = false
                if (ContextCompat.checkSelfPermission(context, cameraPermission) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    launcherCamera.launch(photoUri)
                } else {
                    launcherPermission.launch(cameraPermission)
                }
            },
            onPickGallery = {
                showSheet = false
                launcherGallery.launch("image/*")
            },
            onPickUrl = {
                showSheet = false
                onImageChanged(it.trim())
            },
            onDismiss = { showSheet = false }
        )
    }
}


// ----------------------------
// Фото для рецепта
// ----------------------------
@Composable
fun RecipeImageBlock(
    imageUri: String?,
    onImageChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    // создаём файл для камеры заранее
    val photoFile = remember { createImageFile(context, "main") }
    val photoUri = remember(photoFile) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // Камера -> сохраняем ПУТЬ файла
    val launcherCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageChanged(photoFile.absolutePath) // сохраняем абсолютный путь
        }
    }

    // Галерея -> копируем во внутреннее хранилище и сохраняем ПУТЬ
    val launcherGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.copyUriToInternalStorage(it, "main") { savedPath ->
                if (savedPath != null) onImageChanged(savedPath)
            }
        }
    }

    // Разрешения на камеру
    val cameraPermission = Manifest.permission.CAMERA
    val launcherPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launcherCamera.launch(photoUri)
        } else {
            Toast.makeText(context, "Камера недоступна без разрешения", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .clickable { showSheet = true },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            // Для локального пути безопаснее обернуть в file://
            val model = if (imageUri.startsWith("/")) {
                Uri.fromFile(File(imageUri))
            } else imageUri

            AsyncImage(
                model = model,
                contentDescription = "Recipe Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Text("Добавить фото", color = Color.Gray)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(onClick = { showSheet = true }) {
        Text(if (imageUri == null) "Добавить фото" else "Изменить фото")
    }

    if (showSheet) {
        ImagePickerBottomSheet(
            onPickCamera = {
                showSheet = false
                if (ContextCompat.checkSelfPermission(context, cameraPermission) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    launcherCamera.launch(photoUri)
                } else {
                    launcherPermission.launch(cameraPermission)
                }
            },
            onPickGallery = {
                showSheet = false
                launcherGallery.launch("image/*")
            },
            onPickUrl = { url ->
                showSheet = false
                onImageChanged(url.trim())
            },
            onDismiss = { showSheet = false }
        )
    }
}


// ----------------------------
// BottomSheet выбора фото
// ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onPickUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlText by remember { mutableStateOf("") }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Вставить ссылку на изображение") },
            text = {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/photo.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (urlText.isNotBlank()) {
                        onPickUrl(urlText.trim())
                        urlText = ""
                        showUrlDialog = false
                        onDismiss()
                    }
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Добавить фото",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Button(
                onClick = onPickCamera,
                modifier = Modifier.fillMaxWidth()
            ) { Text("📷 Камера") }
            Button(
                onClick = onPickGallery,
                modifier = Modifier.fillMaxWidth()
            ) { Text("🖼 Галерея") }
            Button(
                onClick = { showUrlDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🌐 Вставить ссылку") }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Отмена") }
        }
    }
}


// ----------------------------
// Теги
// ----------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsInputField(
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTagText by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Заголовок
        Text("Категории", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Список выбранных тегов
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Существующие теги из списка
            allTags.forEach { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag),
                    onClick = {
                        onTagsChanged(
                            if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        )
                    },
                    label = { Text(tag) },
                    leadingIcon = if (selectedTags.contains(tag)) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }

            // Пользовательские теги (не из списка)
            selectedTags.filter { it !in allTags }.forEach { customTag ->
                FilterChip(
                    selected = true,
                    onClick = { onTagsChanged(selectedTags - customTag) },
                    label = { Text(customTag) },
                    leadingIcon = { Icon(Icons.Default.Check, null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле для добавления нового тега
        if (showTagInput) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("Новый тег") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTagText.isNotBlank()) {
                                onTagsChanged(selectedTags + newTagText.trim())
                                newTagText = ""
                                showTagInput = false
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (newTagText.isNotBlank()) {
                            onTagsChanged(selectedTags + newTagText.trim())
                            newTagText = ""
                            showTagInput = false
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, "Добавить")
                }
            }
        } else {
            Button(
                onClick = { showTagInput = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить свой тег")
            }
        }
    }
}