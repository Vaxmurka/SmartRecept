package com.example.smartrecept.data.recipes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val tags: List<String>,
    val time: String,
    val image: String,
    val servings: Int,
    var isFavorite: Boolean,
    val isCooked: Boolean = false,
    val ingredients: List<String>,
    val steps: List<String>,
    var notes: List<String>
)

data class CookingStep(
    val description: String,
    val imageUrl: String? = null,
    val notes: String? = null,
    val timerMinutes: Int? = null,
    var isTimerRunning: Boolean = false
)


// Символы для кодирования заметок (используем редко встречающиеся в тексте)
const val NOTE_START = "|~"
const val NOTE_END = "~|"

fun Recipe.toEnhancedCookingSteps(): List<CookingStep> {
    val stepPattern = """!\[(.*?)\]\((.*?)\)""".toRegex() // ![alt text](image_url)
    val timerPattern = """ (\d+) мин""".toRegex()

    return this.steps.mapIndexed { index, stepText ->
        val (cleanDescription, notes) = extractNotesFromStep(stepText)
        val timerMinutes = timerPattern.find(stepText)?.groupValues?.get(1)?.toIntOrNull()
//        val (imageAlt, imageUrl) = stepPattern.find(stepText)?.destructured ?: Pair("", null)

        CookingStep(
            description = cleanDescription,
            imageUrl = if (index == this.steps.size - 1) this.image else null,
            notes = notes,
            timerMinutes = timerMinutes
        )
    }
}

// Извлекает заметки из текста шага
fun extractNotesFromStep(stepText: String): Pair<String, String?> {
    val noteStartIndex = stepText.indexOf(NOTE_START)
    if (noteStartIndex == -1) return stepText to null

    val noteEndIndex = stepText.indexOf(NOTE_END, noteStartIndex + NOTE_START.length)
    if (noteEndIndex == -1) return stepText to null

    val cleanDescription = stepText.removeRange(noteStartIndex, noteEndIndex + NOTE_END.length)
    val notes = stepText.substring(noteStartIndex + NOTE_START.length, noteEndIndex)

    return cleanDescription.trim() to notes.ifBlank { null }
}

// Добавляет заметки в текст шага
fun addNotesToStep(stepText: String, notes: String?): String {
    return if (notes.isNullOrBlank()) {
        stepText
    } else {
        "$stepText$NOTE_START$notes$NOTE_END"
    }
}
