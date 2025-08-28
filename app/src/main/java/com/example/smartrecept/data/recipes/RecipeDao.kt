package com.example.smartrecept.data.recipes

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    fun getAllFlow(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getFavoritesFlow(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Int): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long // Возвращает новый ID

    @Query("DELETE FROM recipes")
    suspend fun clearAll()

    @Update
    suspend fun update(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE recipes SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("UPDATE recipes SET isCooked = :isCooked WHERE id = :id")
    suspend fun updateCookedStatus(id: Int, isCooked: Boolean)

    @Query("SELECT * FROM recipes WHERE isCooked = 1")
    fun getCookedRecipesFlow(): Flow<List<Recipe>>

    @Query("UPDATE recipes SET notes = :notes WHERE id = :recipeId")
    suspend fun updateNotes(recipeId: Int, notes: List<String>)
}
