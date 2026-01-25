# Экран "Настройки"
1. Доработать смену масштаба (xs, s, m, l, xl)
2. Добавить возможность переключения языков -> выподающий список

# Подумать что делать с экраном **ПОИСК** (убрать его нафиг)

# Дополнительно
1. Сделать возможность создания и чтения рецепта с группами (ИНГРЕДИЕНТЫ ДЛЯ ТЕСТА, ИНГРЕДИЕНТЫ ДЛЯ НАЧИНКИ ...)
2. Сделать возможность добавления видео для рецепта.
3. Добавить слайд шоу на экране рецепта, если это нужно (переключение сделать в настройках)

@Composable
fun FilterChips(
selected: String?,
onSelect: (String?) -> Unit
) {
val options = filterChipsList
Row(
Modifier
.padding(vertical = 8.dp)
.horizontalScroll(rememberScrollState())
//            .padding(horizontal = 16.dp)
) {
options.forEach { label ->
val isSelected = label == selected
AssistChip(
onClick = {
onSelect(if (isSelected) null else label)
},
label = { Text(label) },
colors = AssistChipDefaults.assistChipColors(
containerColor = if (isSelected) getTagColor(label) else MaterialTheme.colorScheme.surfaceVariant
),
modifier = Modifier.padding(end = 8.dp),
border = BorderStroke(
color = Color.Transparent,
width = 1.dp,
)
)
}
}
}

"1.9.22-1.0.17"