package com.example.smartrecept.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    onlyFavorites: Boolean,
    onOnlyFavoritesChange: (Boolean) -> Unit,
    onlyCooked: Boolean,
    onOnlyCookedChange: (Boolean) -> Unit,
    maxTime: Int?,
    onMaxTimeChange: (Int?) -> Unit,
    useAndLogic: Boolean,
    onUseAndLogicChange: (Boolean) -> Unit,
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Фильтры",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Переключатель логики поиска
                Text(
                    text = "Режим поиска",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !useAndLogic,
                        onClick = { onUseAndLogicChange(false) },
                        label = { Text("ИЛИ") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = useAndLogic,
                        onClick = { onUseAndLogicChange(true) },
                        label = { Text("И") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Фильтр по тегам
                Text(
                    text = "Теги",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Горизонтальный скролл тегов
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    tags.forEach { tag ->
                        val isSelected = tag == selectedFilter
                        AssistChip(
                            onClick = {
                                onFilterChange(if (isSelected) null else tag)
                            },
                            label = { Text(tag) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected)
                                    getTagColor(tag)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Дополнительные фильтры
                Text(
                    text = "Дополнительно",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Избранное
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOnlyFavoritesChange(!onlyFavorites) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (onlyFavorites)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Только избранное",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = onlyFavorites,
                            onCheckedChange = onOnlyFavoritesChange
                        )
                    }

                    // Приготовленные
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOnlyCookedChange(!onlyCooked) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = if (onlyCooked)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Только приготовленные",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = onlyCooked,
                            onCheckedChange = onOnlyCookedChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Фильтр по времени
                Text(
                    text = "Время приготовления",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(20, 40, 60, null).forEach { time ->
                        val isSelected = maxTime == time
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMaxTimeChange(time) },
                            label = {
                                Text(
                                    time?.let { "До $it мин" } ?: "Любое время"
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Сброс всех фильтров
                            onFilterChange(null)
                            onOnlyFavoritesChange(false)
                            onOnlyCookedChange(false)
                            onMaxTimeChange(null)
                            onUseAndLogicChange(false)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сбросить")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Применить")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}