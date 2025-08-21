// database/migrations/Migrations.kt
package com.example.smartrecept.data.models.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Создаем новую таблицу с обновленной схемой
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `recipes_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `tags` TEXT NOT NULL,
                    `time` TEXT NOT NULL,
                    `image` TEXT NOT NULL,
                    `servings` TEXT NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `ingredients` TEXT NOT NULL,
                    `steps` TEXT NOT NULL
                )
            """)

            // 2. Копируем данные из старой таблицы в новую
            database.execSQL("""
                INSERT INTO `recipes_new` 
                (id, title, tags, time, image, servings, isFavorite, ingredients, steps)
                SELECT id, title, tags, time, image, servings, isFavorite, ingredients, steps 
                FROM `recipes`
            """)

            // 3. Удаляем старую таблицу
            database.execSQL("DROP TABLE `recipes`")

            // 4. Переименовываем новую таблицу
            database.execSQL("ALTER TABLE `recipes_new` RENAME TO `recipes`")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE recipes ADD COLUMN isCooked INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Создаём временную таблицу с новой схемой
            db.execSQL("""
            CREATE TABLE recipes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                tags TEXT NOT NULL,
                time TEXT NOT NULL,
                image TEXT NOT NULL,
                servings INTEGER NOT NULL DEFAULT 1,
                isFavorite INTEGER NOT NULL,
                isCooked INTEGER NOT NULL DEFAULT 0,
                ingredients TEXT NOT NULL,
                steps TEXT NOT NULL
            )
        """.trimIndent())

            // 2. Копируем данные из старой таблицы, конвертируя servings в Int
            db.execSQL("""
            INSERT INTO recipes_new (
                id, title, tags, time, image, 
                servings, isFavorite, isCooked, ingredients, steps
            )
            SELECT 
                id, title, tags, time, image,
                CASE 
                    WHEN servings GLOB '[0-9]*' THEN CAST(servings AS INTEGER) 
                    ELSE 1 
                END,
                isFavorite, isCooked, ingredients, steps
            FROM recipes
        """.trimIndent())

            // 3. Удаляем старую таблицу
            db.execSQL("DROP TABLE recipes")

            // 4. Переименовываем новую таблицу
            db.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE recipes ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Добавляем поле как NULLABLE
            database.execSQL("ALTER TABLE recipes ADD COLUMN stepImages TEXT")

            // Для существующих записей устанавливаем пустой список
            database.execSQL("UPDATE recipes SET stepImages = '[]' WHERE stepImages IS NULL")
        }
    }
}