package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity

@Dao
interface DiseaseCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<DiseaseCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: DiseaseCategoryEntity)

    @Query("SELECT * FROM disease_categories ORDER BY displayName ASC")
    suspend fun getAllCategories(): List<DiseaseCategoryEntity>

    @Query("SELECT * FROM disease_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): DiseaseCategoryEntity?

    @Query("SELECT COUNT(*) > 0 FROM disease_categories WHERE id = :categoryId")
    suspend fun categoryExists(categoryId: String): Boolean

    @Query("SELECT COUNT(*) FROM disease_categories")
    suspend fun getCategoryCount(): Int
}