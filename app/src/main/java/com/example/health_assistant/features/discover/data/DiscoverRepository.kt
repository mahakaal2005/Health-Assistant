package com.example.health_assistant.features.discover.data

import android.util.Log
import com.example.health_assistant.features.discover.model.HealthTopic
import com.example.health_assistant.features.discover.model.QuickAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository class that handles data operations for the Discover feature
 * Currently using in-memory mock data sources, will be expanded to use Room/network in future
 */
class DiscoverRepository {
    private val TAG = "DiscoverRepository"

    // In-memory storage for mock data
    private val healthTopics = mockHealthTopics()
    private val quickActions = mockQuickActions()

    // In-memory storage for recent searches (max 10)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: Flow<List<String>> = _recentSearches.asStateFlow()

    /**
     * Get a list of featured health topics
     * @return List of featured HealthTopic objects
     */
    fun getFeaturedTopics(): List<HealthTopic> {
        Log.d(TAG, "Getting featured topics, count: ${healthTopics.size}")
        // In a real implementation, this would filter by featured flag or category
        return healthTopics.take(5)
    }

    /**
     * Search for health topics based on the provided query
     * @param query The search text to filter topics by
     * @return List of matching HealthTopic objects
     */
    fun searchTopics(query: String): List<HealthTopic> {
        Log.d(TAG, "Searching for topics with query: $query")
        if (query.isBlank()) {
            return emptyList()
        }

        val results = healthTopics.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.category?.contains(query, ignoreCase = true) == true
        }

        Log.d(TAG, "Found ${results.size} results for query: $query")

        // Add to recent searches if query is not empty and not already in the list
        if (query.isNotBlank()) {
            addToRecentSearches(query)
        }

        return results
    }

    /**
     * Get all available quick actions
     * @return List of QuickAction objects
     */
    fun getQuickActions(): List<QuickAction> {
        Log.d(TAG, "Getting quick actions, count: ${quickActions.size}")
        return quickActions
    }

    /**
     * Add a search query to recent searches
     * @param query The search query to add
     */
    private fun addToRecentSearches(query: String) {
        _recentSearches.update { currentList ->
            val newList = currentList.toMutableList()
            // Remove if already exists (to move it to the front)
            newList.remove(query)
            // Add to front
            newList.add(0, query)
            // Keep only latest 10 searches
            newList.take(10)
        }
        Log.d(TAG, "Added '$query' to recent searches. Total recent searches: ${_recentSearches.value.size}")
    }

    /**
     * Get the list of recent searches
     * @return List of recent search queries
     */
    fun getRecentSearches(): List<String> {
        return _recentSearches.value
    }

    /**
     * Clear all recent searches
     */
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        Log.d(TAG, "Cleared all recent searches")
    }

    // Mock data for development purposes
    private fun mockHealthTopics(): List<HealthTopic> {
        return listOf(
            HealthTopic(
                id = "1",
                title = "Beginner's Guide to Meditation",
                description = "Learn the basics of meditation and mindfulness techniques to reduce stress and improve mental well-being.",
                category = "Mental Health",
                imageUrl = "https://example.com/meditation.jpg"
            ),
            HealthTopic(
                id = "2",
                title = "Understanding Nutrition Labels",
                description = "How to read and interpret nutrition facts labels to make healthier food choices.",
                category = "Nutrition",
                imageUrl = "https://example.com/nutrition.jpg"
            ),
            HealthTopic(
                id = "3",
                title = "Heart-Healthy Exercise Routines",
                description = "Cardio workouts and exercise plans designed to improve cardiovascular health.",
                category = "Fitness",
                imageUrl = "https://example.com/cardio.jpg"
            ),
            HealthTopic(
                id = "4",
                title = "Improving Sleep Quality",
                description = "Tips and techniques for better sleep hygiene and overcoming common sleep problems.",
                category = "Sleep",
                imageUrl = "https://example.com/sleep.jpg"
            ),
            HealthTopic(
                id = "5",
                title = "Stress Management Techniques",
                description = "Practical strategies to manage and reduce stress in daily life.",
                category = "Mental Health",
                imageUrl = "https://example.com/stress.jpg"
            ),
            HealthTopic(
                id = "6",
                title = "Beginner's Guide to Strength Training",
                description = "Learn the fundamentals of strength training with proper form and techniques.",
                category = "Fitness",
                imageUrl = "https://example.com/strength.jpg"
            ),
            HealthTopic(
                id = "7",
                title = "Healthy Meal Prep Strategies",
                description = "Time-saving meal preparation tips to maintain a balanced diet throughout the week.",
                category = "Nutrition",
                imageUrl = "https://example.com/mealprep.jpg"
            ),
            HealthTopic(
                id = "8",
                title = "Understanding Anxiety and Depression",
                description = "Signs, symptoms, and coping strategies for common mental health conditions.",
                category = "Mental Health",
                imageUrl = "https://example.com/anxiety.jpg"
            )
        )
    }

    private fun mockQuickActions(): List<QuickAction> {
        // Using Android system drawable resources that are guaranteed to exist
        return listOf(
            QuickAction(id = "1", title = "Track Mood", iconResId = android.R.drawable.ic_menu_edit),
            QuickAction(id = "2", title = "Log Symptoms", iconResId = android.R.drawable.ic_menu_agenda),
            QuickAction(id = "3", title = "Find Doctor", iconResId = android.R.drawable.ic_menu_search),
            QuickAction(id = "4", title = "Medication Reminder", iconResId = android.R.drawable.ic_popup_reminder),
            QuickAction(id = "5", title = "Track Water Intake", iconResId = android.R.drawable.ic_menu_view),
            QuickAction(id = "6", title = "Daily Steps", iconResId = android.R.drawable.ic_menu_compass)
        )
    }
}