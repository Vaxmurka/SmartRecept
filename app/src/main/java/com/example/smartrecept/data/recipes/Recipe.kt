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

fun Recipe.toEnhancedCookingSteps(): List<CookingStep> {
    val stepPattern = """!\[(.*?)\]\((.*?)\)""".toRegex() // ![alt text](image_url)
    val timerPattern = """ (\d+) мин""".toRegex()

    return this.steps.mapIndexed { index, stepText ->
        val timerMinutes = timerPattern.find(stepText)?.groupValues?.get(1)?.toIntOrNull()
//        val (imageAlt, imageUrl) = stepPattern.find(stepText)?.destructured ?: Pair("", null)

        CookingStep(
            description = stepText,
            imageUrl = if (index == this.steps.size - 1) this.image else null,
            notes = null,
            timerMinutes = timerMinutes
        )
    }
}