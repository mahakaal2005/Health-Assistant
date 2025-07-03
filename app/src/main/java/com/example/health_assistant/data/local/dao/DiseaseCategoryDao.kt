package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseCategoryDao {

    @Query("SELECT * FROM disease_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<DiseaseCategoryEntity>>

    @Query("SELECT * FROM disease_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<DiseaseCategoryEntity>>

    @Query("SELECT * FROM disease_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): DiseaseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: DiseaseCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: DiseaseCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: DiseaseCategoryEntity)

    @Query("UPDATE disease_categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: Long)

    @Query("SELECT * FROM disease_categories WHERE name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<DiseaseCategoryEntity>>
}