import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlin.math.max
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import coil.request.ImageRequest
import com.example.smartrecept.filterChipsList
import com.example.smartrecept.ui.screens.RecipeState
import com.example.smartrecept.ui.screens.RecipeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecipeScreen(
    navController: NavHostController,
    recipeId: Int? = null // null для добавления, не null для редактирования
) {
    // Состояние формы
    var recipeName by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(1) }
    var servingsText by remember { mutableStateOf(servings.toString()) }
    var recipeImage by remember { mutableStateOf<String?>(null) }
    var ingredients = remember { mutableStateListOf("") }
    var steps = remember { mutableStateListOf("") }
    val notes = remember { mutableStateListOf("") }
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    var isInitialLoad by remember { mutableStateOf(true) } // Флаг первоначальной загрузки

    val viewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    // Загружаем конкретный рецепт только при первом показе
    LaunchedEffect(recipeId) {
        if (recipeId != null && isInitialLoad) {
            viewModel.loadRecipe(recipeId)
            isInitialLoad = false
        }
    }

    // Обработчик сохранения
    fun saveRecipe() {
        viewModel.saveRecipe(
            id = recipeId,
            title = recipeName,
            tags = selectedTags,
            time = cookTime,
            image = recipeImage,
            servings = servings,
            ingredients = ingredients.filter { it.isNotBlank() },
            steps = steps.filter { it.isNotBlank() },
            navController = navController,
            notes = notes.filter { it.isNotBlank() },
        )
    }

    val state by viewModel.recipeState.collectAsState()

    // Обновляем форму только при первом успешном получении рецепта
    if (state is RecipeState.Success && !isInitialLoad) {
        val recipe = (state as RecipeState.Success).recipe
        LaunchedEffect(recipe) {
            if (recipeId != null) {
                recipeName = recipe.title
                cookTime = recipe.time
                servings = recipe.servings
                recipeImage = recipe.image
                ingredients.clear()
                ingredients.addAll(recipe.ingredients)
                steps.clear()
                steps.addAll(recipe.steps)
                selectedTags = recipe.tags.toSet()
                notes.clear()
                notes.addAll(recipe.notes)
            }
            isInitialLoad = true // Помечаем, что данные загружены
        }
    }

    // Валидация
    val isFormValid = recipeName.isNotBlank() &&
            cookTime.isNotBlank() &&
            ingredients.all { it.isNotBlank() } &&
            steps.all { it.isNotBlank() }

    LaunchedEffect(servings) { servingsText = servings.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (recipeId == null) "Создание рецепта" else "Изменение рецепта",
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { saveRecipe() },
                        enabled = isFormValid
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Блок изображения
            RecipeImageBlock(
                imageUri = recipeImage,
                onImageSelected = { uri -> recipeImage = uri }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Название рецепта
            OutlinedTextField(
                value = recipeName,
                onValueChange = { recipeName = it },
                label = { Text("Название рецепта") },
                placeholder = { Text("Паста карбонара") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Время и порции
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = cookTime,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cookTime = it },
                    label = { Text("Время") },
                    trailingIcon = { Text("мин") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                            servingsText = newValue
                            newValue.toIntOrNull()?.let { num ->
                                servings = num.coerceAtLeast(1) // Гарантируем минимум 1
                            }
                        }
                    },
                    label = { Text("Порционность") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    servings = max(1, servings - 1)
                                    servingsText = servings.toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, null)
                            }
                            IconButton(
                                onClick = {
                                    servings++
                                    servingsText = servings.toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ингредиенты
            Text("Ингредиенты", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            ingredients.forEachIndexed { index, ingredient ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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

            Button(
                onClick = { ingredients.add("") },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, null)
                Text("Новый ингредиент")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Шаги приготовления
            Text("Шаги", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { steps[index] = it },
                            placeholder = { Text("Напишите что-нибудь...") },
                            modifier = Modifier.weight(1f)
                        )

                        if (ingredients.size > 1) {
                            IconButton(onClick = { steps.removeAt(index) }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                }
            }

            Button(
                onClick = { steps.add("") },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, null)
                Text("Новый шаг")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Теги
            TagsInputField(
                allTags = filterChipsList,
                selectedTags = selectedTags,
                onTagsChanged = { newTags ->
                    selectedTags = newTags
                }
            )
        }
    }
}

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

@Composable
fun RecipeImageBlock(
    imageUri: String?,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val file = remember {
        val imageFile = File(context.cacheDir, "camera_photo.jpg")
        if (!imageFile.exists()) imageFile.createNewFile()
        imageFile
    }
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val launcherCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageSelected(photoUri.toString())
        }
    }

    val launcherGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it.toString()) }
    }

    var showSheet by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray)
                .clickable {
                    showSheet = true
                }
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Recipe Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Add photo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Добавить фото", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showSheet = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (imageUri == null) "Добавить фото" else "Изменить фото")
        }

        if (showSheet) {
            ImagePickerBottomSheet(
                onPickCamera = {
                    launcherCamera.launch(photoUri)
                },
                onPickGallery = {
                    launcherGallery.launch("image/*")
                },
                onDismiss = {
                    showSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Выберите источник",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Divider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "📷 Сделать фото",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPickCamera()
                        onDismiss()
                    }
                    .padding(12.dp)
            )
            Text(
                text = "🖼 Выбрать из галереи",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPickGallery()
                        onDismiss()
                    }
                    .padding(12.dp)
            )
            Text(
                text = "❌ Отмена",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(12.dp),
                color = Color.Red
            )
        }
    }
}