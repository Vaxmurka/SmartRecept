// JsonParser.kt
import org.json.JSONObject
import org.json.JSONArray

data class AIRecipe(
    val airecipe_name: String,
    val ingredients: List<String>,
    val tags: List<String>,
    val time: String,
    val servings: Int,
    val steps: List<String>,
    val notes: List<String>
)

class AIJsonParser {

    companion object {

        /**
         * Парсит JSON строку в объект AIRecipe
         */
        fun parseAIRecipe(jsonString: String): AIRecipe {
            println("Parsing JSON...")
            println("JSON: $jsonString")
            println("Parsing JSON...")
            val json = JSONObject(jsonString)

            return AIRecipe(
                airecipe_name = json.getString("airecipe_name"),
                time = json.getString("time"),
                servings = json.getInt("servings"),
                ingredients = parseStringArray(json.getJSONArray("ingredients")),
                tags = parseStringArray(json.getJSONArray("tags")),
                steps = parseStringArray(json.getJSONArray("steps")),
                notes = parseStringArray(json.getJSONArray("notes"))
            )
        }

        /**
         * Преобразует JSONArray в List<String>
         */
        private fun parseStringArray(jsonArray: JSONArray): List<String> {
            return List(jsonArray.length()) { index ->
                jsonArray.getString(index)
            }
        }

        /**
         * Альтернативный метод - возвращает все элементы как Map
         */
        fun parseToMap(jsonString: String): Map<String, Any> {
            val json = JSONObject(jsonString)
            val map = mutableMapOf<String, Any>()

            map["AIRecipe_name"] = json.getString("airecipe_name")
            map["time"] = json.getString("time")
            map["servings"] = json.getInt("servings")
            map["ingredients"] = parseStringArray(json.getJSONArray("ingredients"))
            map["tags"] = parseStringArray(json.getJSONArray("tags"))
            map["steps"] = parseStringArray(json.getJSONArray("steps"))
            map["notes"] = parseStringArray(json.getJSONArray("notes"))

            return map
        }

        /**
         * Выводит информацию о рецепте в консоль
         */
        fun printAIRecipeInfo(jsonString: String) {
            val AIRecipe = parseAIRecipe(jsonString)

            println("=".repeat(50))
            println("РЕЦЕПТ: ${AIRecipe.airecipe_name}")
            println("=".repeat(50))

            println("\n📊 ОСНОВНАЯ ИНФОРМАЦИЯ:")
            println("⏱️  Время приготовления: ${AIRecipe.time} минут")
            println("👥 Порций: ${AIRecipe.servings}")

            println("\n🏷️  ТЕГИ:")
            AIRecipe.tags.forEach { tag ->
                println("   #$tag")
            }

            println("\n🛒 ИНГРЕДИЕНТЫ:")
            AIRecipe.ingredients.forEachIndexed { index, ingredient ->
                println("   ${index + 1}. $ingredient")
            }

            println("\n👨‍🍳 ШАГИ ПРИГОТОВЛЕНИЯ:")
            AIRecipe.steps.forEachIndexed { index, step ->
                println("\n   Шаг ${index + 1}:")
                println("   $step")
            }

            println("\n💡 ПРИМЕЧАНИЯ:")
            AIRecipe.notes.forEachIndexed { index, note ->
                println("   ${index + 1}. $note")
            }
            println("=".repeat(50))
        }
    }
}