package com.example.smartrecept.data.recipes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartrecept.data.models.migrations.DatabaseMigrations

@Database(entities = [Recipe::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "recipes_db"
            )
                .addMigrations(DatabaseMigrations.MIGRATION_4_5)
                .build()
        }
    }
}
