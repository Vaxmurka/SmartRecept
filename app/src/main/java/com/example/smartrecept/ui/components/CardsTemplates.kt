// ui/components/CardTemplates.kt
package com.example.smartrecept.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.smartrecept.data.recipes.Recipe
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.smartrecept.Screen
import kotlin.math.abs


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    isFavorite: Boolean,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onHomePage: Boolean = false,
    onToggleFavorite: () -> Unit,
    onEdit: (Recipe) -> Unit,
    onDelete: (Recipe) -> Unit,
    onCookedToggle: (Boolean) -> Unit = {}
) {
    var isMenuVisible by remember { mutableStateOf(false) }
    val longPressOffset by remember { mutableStateOf(Offset.Zero) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = { navController.navigate("recipe/${recipe.id}") },
                onLongClick = { isMenuVisible = true }
            )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.image)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    placeholder = ColorPainter(Color.LightGray),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val tags by remember(recipe.tags) {
                            derivedStateOf { recipe.tags.filter { it.isNotBlank() } }
                        }

                        tags.forEach { tag ->
                            AssistChip(
                                onClick = {}, // без действия
                                label = {
                                    Text(
                                        tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = getTagColor(tag)
                                ),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .height(18.dp),
                                border = BorderStroke(
                                    color = Color.Transparent,
                                    width = 1.dp,
                                )
                            )
                        }
                    }
                }

                if (!onHomePage) {
                    IconButton(onClick = { onToggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = { navController.navigate("recipe/${recipe.id}") }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        // Контекстное меню
        DropdownMenu(
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
            offset = with(density) {
                DpOffset(
                    x = longPressOffset.x.toDp(),
                    y = longPressOffset.y.toDp()
                )
            }
        ) {
            DropdownMenuItem(
                text = { Text("Редактировать") },
                onClick = {
                    isMenuVisible = false
                    onEdit(recipe)
                }
            )
            DropdownMenuItem(
                text = { Text(if (recipe.isCooked) "Удалить из приготовленных" else "Готовил") },
                onClick = {
                    isMenuVisible = false
                    onCookedToggle(!recipe.isCooked)
                }
            )
            DropdownMenuItem(
                text = { Text(if (recipe.isFavorite) "Удалить из избранного" else "Добавить в избранное") },
                onClick = {
                    isMenuVisible = false
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    isMenuVisible = false
                    showDeleteDialog = true
                },
                colors = MenuItemColors(
                    textColor = Color.Red,
                    leadingIconColor = Color.LightGray,
                    trailingIconColor = Color.LightGray,
                    disabledTextColor = Color.LightGray,
                    disabledLeadingIconColor = Color.LightGray,
                    disabledTrailingIconColor = Color.LightGray
                )
            )
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(recipe)
                    showDeleteDialog = false
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            },
            title = { Text("Удалить рецепт?") },
            text = { Text("Вы уверены, что хотите удалить этот рецепт?") }
        )
    }
}

@Composable
fun RecipeDayCard(recipe: Recipe, navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("recipe/${recipe.id}") }, // переход,
        contentAlignment = Alignment.BottomStart
    ) {
        AsyncImage(
            model = recipe.image,
            contentDescription = recipe.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 28.dp, bottomEnd = 28.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(8.dp)
        )
    }
}

@Composable
fun CustomSearchPanel(
    query: String,
    readOnly: Boolean = false,
    onQueryChange: (String) -> Unit = {},
    navController: NavHostController,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .heightIn(min = 56.dp),  // Минимальная высота для выравнивания
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кастомизированное поле поиска
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .height(50.dp)  // Фиксированная высота для TextField
                .clip(MaterialTheme.shapes.medium)  // Скругление углов
                .background(MaterialTheme.colorScheme.surfaceVariant)  // Фон
                .align(Alignment.CenterVertically)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                readOnly = readOnly,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier.fillMaxSize()
                    .clickable { if (readOnly) navController.navigate("search") }
            )
        }

        // Кнопка избранного
        IconButton(
            onClick = { navController.navigate(Screen.Favorites.route) },
            modifier = Modifier.size(50.dp)
                .clip(MaterialTheme.shapes.medium)  // Скругление углов
                .background(MaterialTheme.colorScheme.surfaceVariant)  // Фон
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Избранное",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CustomCard(
    modifier: Modifier = Modifier,
    boxPadding: PaddingValues = PaddingValues(16.dp),
    outPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.padding(outPadding).clickable { onClick() }
    } else {
        modifier.padding(outPadding)
    }

    Card(
        modifier = clickableModifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(elevation),
        border = border
    ) {
        Box(
            modifier = Modifier.padding(boxPadding),
            contentAlignment = Alignment.TopStart
        ) {
            content()
        }
    }
}

@Composable
fun CollapsibleCard(
    title: String,
    modifier: Modifier = Modifier,
    boxPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    outPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth().padding(outPadding),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Заголовок с кнопкой сворачивания
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Анимированное содержимое
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.padding(boxPadding)
                ) {
                    content()
                }
            }
        }
    }
}

fun getTagColor(tag: String): Color {
    val hash = tag.hashCode()
    val hue = (hash % 360).toFloat() // от 0 до 360 градусов по кругу
    val saturation = 0.2f            // пастельность
    val value = 0.7f                 // яркость

    val c = value * saturation
    val x = c * (1 - abs((hue / 60f) % 2 - 1))
    val m = value - c

    val (r1, g1, b1) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

    return Color(r, g, b)
}