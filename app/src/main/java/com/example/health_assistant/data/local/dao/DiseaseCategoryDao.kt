package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for disease category operations
 * Handles categorization of medical conditions and diseases
 */
@Dao
interface DiseaseCategoryDao {

    @Query("SELECT * FROM disease_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<DiseaseCategoryEntity>>

    @Query("SELECT * FROM disease_categories ORDER BY id DESC")
    fun getAllCategories(): Flow<List<DiseaseCategoryEntity>>

    @Query("SELECT * FROM disease_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): DiseaseCategoryEntity?

    @Query("SELECT * FROM disease_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveCategories(): Flow<List<DiseaseCategoryEntity>>

    @Query("SELECT * FROM disease_categories WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%'")
    fun searchCategories(searchQuery: String): Flow<List<DiseaseCategoryEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: DiseaseCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<DiseaseCategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: DiseaseCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: DiseaseCategoryEntity)

    @Query("DELETE FROM disease_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("UPDATE disease_categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: Long)

    @Query("SELECT COUNT(*) FROM disease_categories WHERE isActive = 1")
    suspend fun getActiveCategoryCount(): Int

    @Query("DELETE FROM disease_categories")
    suspend fun deleteAllCategories()
}