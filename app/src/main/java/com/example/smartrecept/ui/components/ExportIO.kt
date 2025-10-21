// ui.component/ExportIO.kt
package com.example.smartrecept.ui.components

import android.util.Log
import com.example.smartrecept.data.recipes.Recipe
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportIO {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    // ---------- JSON ----------
    fun exportToJson(recipes: List<Recipe>): String {
        return gson.toJson(recipes)
    }

    fun importFromJson(json: String): List<Recipe> {
        return try {
            val type = object : TypeToken<List<Recipe>>() {}.type
            val recipes: List<Recipe> = Gson().fromJson(json, type)

            Log.d("ImportDebug", "Успешно отпарсено ${recipes.size} рецептов из JSON.")
            recipes.forEachIndexed { index, recipe ->
                Log.d("ImportDebug", "Рецепт #${index + 1}: $recipe")
            }
            // ---------------------------------------------

            recipes
        } catch (e: Exception) {
            Log.e("ImportDebug", "Ошибка парсинга JSON: ${e.message}", e)
            // ---------------------------------------
            emptyList() // Возвращаем пустой список в случае ошибки
        }
    }

    // ---------- CSV ----------
    fun exportToCsv(recipes: List<Recipe>): String {
        val csv = StringBuilder()

        // Заголовок CSV
        csv.append("ID,Title,Tags,Time,Image,Servings,IsFavorite,IsCooked,Ingredients,Steps,Notes,StepImages\n")

        recipes.forEach { recipe ->
            try {
                // Преобразуем списки в строки с разделителем "|"
                val tagsCsv = recipe.tags.joinToString("|")
                val ingredientsCsv = recipe.ingredients.joinToString("|")
                val stepsCsv = recipe.steps.joinToString("|")
                val notesCsv = recipe.notes.joinToString("|")
                // Для stepImages обрабатываем null значения, заменяя их на пустую строку
                val stepImagesCsv = recipe.stepImages.joinToString("|") { it ?: "" }

                // Формируем строку для рецепта, экранируя значения в кавычки
                val recipeRow = listOf(
                    recipe.id,
                    recipe.title,
                    tagsCsv,
                    recipe.time,
                    recipe.image,
                    recipe.servings,
                    recipe.isFavorite,
                    recipe.isCooked,
                    ingredientsCsv,
                    stepsCsv,
                    notesCsv,
                    stepImagesCsv
                ).joinToString(",") { escapeCsvField(it.toString()) } // Оборачиваем каждое поле

                csv.appendLine(recipeRow)

            } catch (e: Exception) {
                Log.e("ExportDebug", "Ошибка при конвертации рецепта #${recipe.id} в CSV: ${e.message}", e)
                // Пропускаем этот рецепт, чтобы не сломать весь экспорт
            }
        }
        println(csv.toString())
        return csv.toString()
    }

    fun importFromCsv(csv: String): List<Recipe> {
        // Получаем все строки, кроме заголовка, и отфильтровываем пустые
        val lines = csv.lines().drop(1).filter { it.isNotBlank() }

        return lines.mapNotNull { line ->
            try {
                // Используем умный парсер для одной строки CSV
                val parts = parseCsvLine(line)

                // Проверяем, что в строке нужное количество колонок (12, как в заголовке)
                if (parts.size < 12) {
                    Log.w("ImportDebug", "Пропущена строка: неверное кол-во колонок (${parts.size}). Строка: $line")
                    return@mapNotNull null // Пропускаем некорректную строку
                }

                // Теперь безопасно разбираем каждую часть
                Recipe(
                    id = parts[0].toIntOrNull() ?: 0,
                    title = parts[1],
                    // Восстанавливаем списки, разделяя строку по '|'
                    tags = if (parts[2].isNotBlank()) parts[2].split('|') else emptyList(),
                    time = parts[3],
                    image = parts[4],
                    servings = parts[5].toIntOrNull() ?: 0,
                    isFavorite = parts[6].toBoolean(),
                    isCooked = parts[7].toBoolean(),
                    ingredients = if (parts[8].isNotBlank()) parts[8].split('|') else emptyList(),
                    steps = if (parts[9].isNotBlank()) parts[9].split('|') else emptyList(),
                    notes = if (parts[10].isNotBlank()) parts[10].split('|') else emptyList(),
                    stepImages = if (parts[11].isNotBlank()) {
                        parts[11].split('|').map { if (it.isEmpty()) null else it }
                    } else {
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e("ImportDebug", "Ошибка парсинга CSV строки: '$line'", e)
                null // Если в строке ошибка, пропускаем ее и возвращаем null
            }
        }
    }

    /**
     * Вспомогательная функция для корректного разбора одной строки CSV.
     * Она умеет обрабатывать поля, заключенные в кавычки, и запятые внутри них.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val builder = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                // Двойная кавычка внутри поля в кавычках (экранирование)
                inQuotes && char == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    builder.append('"')
                    i++ // Пропускаем вторую кавычку
                }
                // Начало или конец поля в кавычках
                char == '"' -> inQuotes = !inQuotes
                // Разделитель-запятая, но только если мы не внутри кавычек
                char == ',' && !inQuotes -> {
                    result.add(builder.toString()) // Завершаем поле
                    builder.clear() // Очищаем для следующего поля
                }
                // Обычный символ
                else -> builder.append(char)
            }
            i++
        }
        result.add(builder.toString()) // Добавляем самое последнее поле
        return result
    }

    // ---------- Вспомогательные ----------
    private fun String.escapeCsv(): String =
        replace("\"", "\"\"").replace(";", ",")

    private fun String.unescapeCsv(): String =
        replace("\"\"", "\"").replace(",", ";")

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    fun generateFileName(extension: String): String {
        val timestamp = dateFormatter.format(Date())
        return "recipes_${timestamp}.${extension}"
    }
}