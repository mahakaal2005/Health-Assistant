package com.example.health_assistant.data.manager

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple category manager that handles both predefined and user-created categories
 */
@Singleton
class CategoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prescription_categories", Context.MODE_PRIVATE)

    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    val allCategories: StateFlow<List<String>> = _allCategories.asStateFlow()

    // Predefined categories
    private val defaultCategories = listOf(
        "Pain Relief",
        "Antibiotics",
        "Heart & Blood Pressure",
        "Diabetes",
        "Mental Health",
        "Vitamins & Supplements",
        "Respiratory",
        "Digestive",
        "Skin Care",
        "Eye Care",
        "Allergy",
        "Other"
    )

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val customCategories = getCustomCategories()
        val combined = (defaultCategories + customCategories).distinct().sorted()
        _allCategories.value = combined
    }

    /**
     * Get all available categories (predefined + custom)
     */
    fun getAllCategories(): List<String> {
        return _allCategories.value
    }

    /**
     * Add a custom category if it doesn't exist
     */
    fun addCustomCategory(category: String): Boolean {
        val trimmedCategory = category.trim()
        if (trimmedCategory.isBlank()) return false

        // Check if category already exists (case-insensitive)
        val existingCategories = getAllCategories()
        if (existingCategories.any { it.equals(trimmedCategory, ignoreCase = true) }) {
            return false // Category already exists
        }

        // Add to custom categories
        val customCategories = getCustomCategories().toMutableList()
        customCategories.add(trimmedCategory)
        saveCustomCategories(customCategories)

        // Reload all categories
        loadCategories()
        return true
    }

    /**
     * Get categories for dropdown with "Add Custom..." option
     */
    fun getCategoriesForDropdown(): List<String> {
        return getAllCategories() + listOf("➕ Add Custom Category...")
    }

    /**
     * Get categories for filter (with "All Categories" option)
     */
    fun getCategoriesForFilter(): List<String> {
        return listOf("All Categories") + getAllCategories()
    }

    private fun getCustomCategories(): List<String> {
        val categoriesString = prefs.getString("custom_categories", "") ?: ""
        return if (categoriesString.isBlank()) {
            emptyList()
        } else {
            categoriesString.split(",").filter { it.isNotBlank() }
        }
    }

    private fun saveCustomCategories(categories: List<String>) {
        prefs.edit()
            .putString("custom_categories", categories.joinToString(","))
            .apply()
    }
}