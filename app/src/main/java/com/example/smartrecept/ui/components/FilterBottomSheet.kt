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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smartrecept.R

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
                        text = stringResource(R.string.filters_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Переключатель логики поиска
                Text(
                    text = stringResource(R.string.search_mode),
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
                        label = { Text(stringResource(R.string.or_mode)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = useAndLogic,
                        onClick = { onUseAndLogicChange(true) },
                        label = { Text(stringResource(R.string.and_mode)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Фильтр по тегам
                Text(
                    text = stringResource(R.string.tags_title),
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
                    text = stringResource(R.string.additional_filters),
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
                            text = stringResource(R.string.only_favorites),
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
                            text = stringResource(R.string.only_cooked),
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
                    text = stringResource(R.string.cooking_time_filter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeOptions = listOf(20, 40, 60, null)
                    timeOptions.forEach { time ->
                        val isSelected = maxTime == time
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMaxTimeChange(time) },
                            label = {
                                Text(
                                    time?.let { stringResource(R.string.up_to_minutes, it) }
                                        ?: stringResource(R.string.any_time)
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
                        Text(stringResource(R.string.reset))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}