package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for disease categories
 * Provides CRUD operations for prescription categories with reactive Flow support
 */
@Dao
interface DiseaseCategoryDao {

    /**
     * Get all active disease categories ordered by sort order
     */
    @Query("SELECT * FROM disease_categories WHERE isActive = 1 ORDER BY sortOrder ASC, displayName ASC")
    fun getAllCategoriesFlow(): Flow<List<DiseaseCategoryEntity>>

    /**
     * Get all active disease categories as suspend function
     */
    @Query("SELECT * FROM disease_categories WHERE isActive = 1 ORDER BY sortOrder ASC, displayName ASC")
    suspend fun getAllCategories(): List<DiseaseCategoryEntity>

    /**
     * Get category by ID
     */
    @Query("SELECT * FROM disease_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): DiseaseCategoryEntity?

    /**
     * Get category by ID as Flow for reactive updates
     */
    @Query("SELECT * FROM disease_categories WHERE id = :categoryId")
    fun getCategoryByIdFlow(categoryId: String): Flow<DiseaseCategoryEntity?>

    /**
     * Get all custom categories created by user
     */
    @Query("SELECT * FROM disease_categories WHERE isCustom = 1 AND isActive = 1 ORDER BY displayName ASC")
    suspend fun getCustomCategories(): List<DiseaseCategoryEntity>

    /**
     * Insert single category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: DiseaseCategoryEntity)

    /**
     * Insert multiple categories (for seeding default data)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<DiseaseCategoryEntity>)

    /**
     * Update category
     */
    @Update
    suspend fun updateCategory(category: DiseaseCategoryEntity)

    /**
     * Soft delete category (mark as inactive)
     */
    @Query("UPDATE disease_categories SET isActive = 0, updatedAt = :timestamp WHERE id = :categoryId")
    suspend fun softDeleteCategory(categoryId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Hard delete category (permanent removal)
     */
    @Query("DELETE FROM disease_categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    /**
     * Delete all categories (for cleanup/reset)
     */
    @Query("DELETE FROM disease_categories")
    suspend fun deleteAllCategories()

    /**
     * Check if category exists
     */
    @Query("SELECT COUNT(*) FROM disease_categories WHERE id = :categoryId")
    suspend fun categoryExists(categoryId: String): Int

    /**
     * Get category count
     */
    @Query("SELECT COUNT(*) FROM disease_categories WHERE isActive = 1")
    suspend fun getActiveCategoryCount(): Int

    /**
     * Search categories by name
     */
    @Query("SELECT * FROM disease_categories WHERE (displayName LIKE '%' || :searchQuery || '%' OR name LIKE '%' || :searchQuery || '%') AND isActive = 1 ORDER BY displayName ASC")
    suspend fun searchCategories(searchQuery: String): List<DiseaseCategoryEntity>
}