// data/recipes/DatasourceRecipes.kt
package com.example.smartrecept.data.recipes
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

val catalogRecipes = listOf(
    Recipe(
        id = 1,
        title = "Куриный суп с лапшой",
        tags = listOf("Суп", "Быстрое"),
        time = "30",
        image = "https://scdn.chibbis.ru/live/products/e64b4c7b00a2ed09ef00bf47676459bb.jpeg",
        servings = 2,
        isFavorite = true,
        ingredients = listOf(
            "Куриное филе – 300 г",
            "Лапша – 100 г",
            "Морковь – 1 шт.",
            "Картофель – 2 шт.",
            "Лук – 1 шт.",
            "Соль – по вкусу",
            "Перец – по вкусу",
            "Зелень – для подачи"
        ),
        steps = listOf(
            "Порежь куриное филе кубиками и отвари в подсоленной воде 15 минут.",
            "Добавь нарезанный картофель и морковь, вари 10 минут.",
            "Положи лапшу и мелко нарезанный лук, вари ещё 5 минут.",
            "Приправь по вкусу, укрась зеленью и подавай."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 2,
        title = "Салат Цезарь с курицей",
        tags = listOf("Салат", "Праздничное"),
        time = "20",
        image = "https://hlebvarteme.ru/images/салат-цезарь-с-курицей.jpg",
        servings = 2,
        isFavorite = false,
        ingredients = listOf(
            "Куриная грудка – 200 г",
            "Салат Романо – 1 пучок",
            "Помидоры черри – 6 шт.",
            "Сыр Пармезан – 30 г",
            "Сухарики – 50 г",
            "Соус Цезарь – 3 ст. ложки",
            "Оливковое масло – 1 ст. ложка",
            "Соль, перец – по вкусу"
        ),
        steps = listOf(
            "Обжарь куриную грудку на сковороде до золотистой корочки.",
            "Порви листья салата руками и выложи на тарелку.",
            "Добавь нарезанные черри, курицу и сухарики.",
            "Полей соусом и укрась тертым пармезаном."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 3,
        title = "Борщ",
        tags = listOf("Суп", "Традиционное"),
        time = "60",
        image = "https://scdn.chibbis.ru/live/products/ded147cbd548f59861c6d9127de3455e.jpeg",
        servings = 4,
        isFavorite = false,
        ingredients = listOf(
            "Свёкла – 2 шт.",
            "Картофель – 3 шт.",
            "Морковь – 1 шт.",
            "Лук – 1 шт.",
            "Капуста – 200 г",
            "Томатная паста – 2 ст. л.",
            "Говядина – 300 г",
            "Соль, перец – по вкусу",
            "Сметана – для подачи"
        ),
        steps = listOf(
            "Отвари говядину в подсоленной воде до готовности.",
            "Обжарь лук, морковь и свёклу с томатной пастой.",
            "Добавь овощи и картофель в бульон, вари 15 минут.",
            "Добавь капусту, вари ещё 10 минут.",
            "Подавай со сметаной и зеленью."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 4,
        title = "Пельмени домашние",
        tags = listOf("Основное", "Традиционное"),
        time = "90",
        image = "https://avatars.dzeninfra.ru/get-zen_doc/271828/pub_6752e16bc2d1857fb4bb7891_675aaf3ac914885a3804f11f/scale_1200",
        servings = 4,
        isFavorite = true,
        ingredients = listOf(
            "Мука – 400 г",
            "Яйцо – 1 шт.",
            "Вода – 150 мл",
            "Говядина – 200 г",
            "Свинина – 200 г",
            "Лук – 1 шт.",
            "Соль, перец – по вкусу"
        ),
        steps = listOf(
            "Замеси тесто из муки, яйца и воды, оставь на 30 минут.",
            "Сделай фарш из мяса и лука, посоли и поперчи.",
            "Раскатай тесто, вырежи кружки, заверни начинку.",
            "Отвари в подсоленной воде до всплытия + 5 минут.",
            "Подавай со сметаной или уксусом."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 5,
        title = "Оливье",
        tags = listOf("Салат", "Праздничное"),
        time = "25",
        image = "https://cdn.lifehacker.ru/wp-content/uploads/2023/11/Olive-s-avokado_1701255010-e1701255050296.jpg",
        servings = 4,
        isFavorite = false,
        ingredients = listOf(
            "Картофель – 3 шт.",
            "Морковь – 1 шт.",
            "Яйца – 3 шт.",
            "Колбаса – 300 г",
            "Огурцы солёные – 2 шт.",
            "Зелёный горошек – 150 г",
            "Майонез – 3 ст. ложки"
        ),
        steps = listOf(
            "Отвари картофель, морковь и яйца.",
            "Охлади и нарежь всё кубиками.",
            "Добавь горошек и майонез, перемешай.",
            "Охлади в холодильнике перед подачей."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 6,
        title = "Блины классические",
        tags = listOf("Завтрак", "Сладкое"),
        time = "35",
        image = "https://object.pscloud.io/cms/cms/Uploads/34224790_W047oT.jpg",
        servings = 10,
        isFavorite = true,
        ingredients = listOf(
            "Молоко – 500 мл",
            "Яйца – 2 шт.",
            "Мука – 200 г",
            "Сахар – 1 ст. ложка",
            "Соль – 0.5 ч. ложки",
            "Масло растительное – 2 ст. ложки"
        ),
        steps = listOf(
            "Смешай яйца, молоко, сахар и соль.",
            "Постепенно добавь муку, размешай.",
            "Добавь масло, жарь блины на сковороде.",
            "Подавай с вареньем или сметаной."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 7,
        title = "Бефстроганов",
        tags = listOf("Основное", "Быстрое"),
        time = "30",
        image = "https://avatars.dzeninfra.ru/get-zen_doc/271828/pub_66326cf012b8d64870c6aee6_66326dfe71b8677f1da2d751/scale_1200",
        servings = 3,
        isFavorite = false,
        ingredients = listOf(
            "Говядина – 400 г",
            "Лук – 1 шт.",
            "Сметана – 150 г",
            "Горчица – 1 ч. ложка",
            "Масло – для жарки",
            "Соль, перец – по вкусу"
        ),
        steps = listOf(
            "Нарежь мясо тонкими полосками, обжарь до золотистого цвета.",
            "Добавь лук, обжарь до мягкости.",
            "Влей сметану с горчицей, туши 10 минут.",
            "Подавай с картофельным пюре или рисом."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 8,
        title = "Медовик",
        tags = listOf("Десерт", "Праздничное"),
        time = "120",
        image = "https://www.gdebar.ru/data/app/bar/img/gallery/8522/216715.webp",
        servings = 8,
        isFavorite = false,
        ingredients = listOf(
            "Яйца – 3 шт.",
            "Сахар – 200 г",
            "Мёд – 2 ст. ложки",
            "Мука – 400 г",
            "Сметана – 400 г"
        ),
        steps = listOf(
            "Взбей яйца с сахаром и мёдом, добавь муку, замеси тесто.",
            "Раздели на 6–8 коржей, испеки в духовке.",
            "Промажь сметаной, оставь пропитаться на ночь.",
            "Укрась по желанию орехами или крошкой."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 9,
        title = "Гречка с грибами",
        tags = listOf("Гарнир", "Постное"),
        time = "30",
        image = "https://gipfel.ru/upload/iblock/6a3/0h4yv2q51p0y6md8a1w4c5zjfsuc3dod.jpg",
        servings = 2,
        isFavorite = true,
        ingredients = listOf(
            "Гречка – 1 стакан",
            "Грибы шампиньоны – 200 г",
            "Лук – 1 шт.",
            "Масло – для жарки",
            "Соль – по вкусу"
        ),
        steps = listOf(
            "Промой гречку и вари до готовности.",
            "Обжарь лук и грибы до золотистого цвета.",
            "Смешай с готовой гречкой, приправь."
        ),
        notes = listOf()
    ),
    Recipe(
        id = 10,
        title = "Вареники с вишней",
        tags = listOf("Десерт", "Постное"),
        time = "50",
        image = "https://avatars.mds.yandex.net/get-marketcms/475644/img-0a578845-e099-49eb-9b01-1e36dad24394.jpeg/optimize",
        servings = 3,
        isFavorite = false,
        ingredients = listOf(
            "Мука – 300 г",
            "Вода – 150 мл",
            "Вишня без косточек – 300 г",
            "Сахар – 3 ст. ложки"
        ),
        steps = listOf(
            "Замеси тесто из воды и муки, оставь на 20 минут.",
            "Сформируй вареники с вишней и сахаром.",
            "Отвари в подсоленной воде 5–7 минут.",
            "Подавай со сметаной или сахаром."
        ),
        notes = listOf()
    )
)


class DatasourceRecipes(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val recipeDao = database.recipeDao()

    suspend fun loadRecipes(): List<Recipe> {
        return recipeDao.getAll()
    }

    fun loadRecipesFlow(): Flow<List<Recipe>> {
        return recipeDao.getAllFlow()
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        return recipeDao.getById(id)
    }

    fun getFavoritesFlow(): Flow<List<Recipe>> {
        return recipeDao.getFavoritesFlow()
    }

    suspend fun updateFavoriteRecipe(id: Int, isFavorite: Boolean) {
        recipeDao.updateFavorite(id, isFavorite)
    }

    suspend fun insertRecipe(recipe: Recipe) {
        recipeDao.insert(recipe)
    }

    suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.update(recipe)
    }

    suspend fun isFavorite(id: Int): Boolean {
        return recipeDao.getById(id)?.isFavorite ?: false
    }

    suspend fun deleteRecipe(recipeId: Int) {
        return recipeDao.deleteById(recipeId)
    }

    suspend fun initializeDatabase() {
        // Проверяем, есть ли уже рецепты в базе
        val existingRecipes = recipeDao.getAll()

        // Если база пустая, загружаем наш каталог
        if (existingRecipes.isEmpty()) {
            recipeDao.insertAll(catalogRecipes)
            Log.d("Database", "Inserted ${catalogRecipes.size} recipes")
        } else {
            // Если нужно обновить существующие рецепты
            val newRecipes = catalogRecipes.filter { newRecipe ->
                existingRecipes.none { it.id == newRecipe.id }
            }

            if (newRecipes.isNotEmpty()) {
                recipeDao.insertAll(newRecipes)
                Log.d("Database", "Inserted ${newRecipes.size} new recipes")
            }
        }
    }

    suspend fun clearAllFavorites() {
        recipeDao.clearAllFavorites()
    }

    suspend fun updateCookedStatus(id: Int, isCooked: Boolean) {
        recipeDao.updateCookedStatus(id, isCooked)
    }

    fun getCookedRecipesFlow(): Flow<List<Recipe>> {
        return recipeDao.getCookedRecipesFlow()
    }

    suspend fun updateRecipeNotes(recipeId: Int, notes: List<String>) {
        recipeDao.updateNotes(recipeId, notes)
    }

    suspend fun replaceAllRecipesWithCatalog() {
        withContext(Dispatchers.IO) {
            // 1. Очищаем базу данных
            recipeDao.clearAll()

            // 2. Вставляем все рецепты из каталога
            recipeDao.insertAll(catalogRecipes)

            // 3. Логируем результат
            Log.d("Database", "Database replaced with ${catalogRecipes.size} recipes")
        }
    }
}