package com.example.health_assistant.features.discover.domain.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.ABTestEntity
import org.json.JSONObject
import org.json.JSONArray
import kotlin.random.Random

/**
 * A/B Testing framework for optimizing content presentation
 * Manages test assignment, tracking, and performance analysis
 */
@Singleton
class ABTestManager @Inject constructor(
    private val analyticsDao: AnalyticsDao
) {
    
    companion object {
        // Active A/B tests configuration
        private val ACTIVE_TESTS = mapOf(
            "content_layout_v1" to ABTestConfig(
                name = "content_layout_v1",
                variants = listOf("card_layout", "list_layout", "magazine_layout"),
                trafficAllocation = mapOf(
                    "card_layout" to 0.4f,
                    "list_layout" to 0.4f,
                    "magazine_layout" to 0.2f
                ),
                conversionEvents = listOf("content_click", "reading_complete", "bookmark"),
                startDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), // Started 7 days ago
                endDate = System.currentTimeMillis() + (23 * 24 * 60 * 60 * 1000L) // Ends in 23 days
            ),
            "recommendation_algorithm_v2" to ABTestConfig(
                name = "recommendation_algorithm_v2",
                variants = listOf("collaborative_filtering", "content_based", "hybrid"),
                trafficAllocation = mapOf(
                    "collaborative_filtering" to 0.33f,
                    "content_based" to 0.33f,
                    "hybrid" to 0.34f
                ),
                conversionEvents = listOf("recommendation_click", "recommendation_bookmark"),
                startDate = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L), // Started 3 days ago
                endDate = System.currentTimeMillis() + (27 * 24 * 60 * 60 * 1000L) // Ends in 27 days
            ),
            "reading_progress_indicator" to ABTestConfig(
                name = "reading_progress_indicator",
                variants = listOf("progress_bar", "percentage_text", "time_remaining", "control"),
                trafficAllocation = mapOf(
                    "progress_bar" to 0.3f,
                    "percentage_text" to 0.25f,
                    "time_remaining" to 0.25f,
                    "control" to 0.2f
                ),
                conversionEvents = listOf("reading_complete", "session_duration"),
                startDate = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L), // Started 1 day ago
                endDate = System.currentTimeMillis() + (29 * 24 * 60 * 60 * 1000L) // Ends in 29 days
            )
        )
    }
    
    /**
     * Get or assign user to A/B test variant
     */
    suspend fun getTestVariant(userId: String, testName: String): String? = withContext(Dispatchers.IO) {
        val testConfig = ACTIVE_TESTS[testName] ?: return@withContext null
        val currentTime = System.currentTimeMillis()
        
        // Check if test is active
        if (currentTime < testConfig.startDate || currentTime > testConfig.endDate) {
            return@withContext null
        }
        
        // Check if user is already assigned to this test
        val existingTest = analyticsDao.getActiveABTest(userId, testName)
        if (existingTest != null) {
            return@withContext existingTest.variant
        }
        
        // Assign user to variant based on traffic allocation
        val variant = assignUserToVariant(userId, testConfig)
        
        // Create new A/B test record
        val abTest = ABTestEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            testName = testName,
            variant = variant,
            isActive = true,
            assignedAt = currentTime,
            startDate = testConfig.startDate,
            endDate = testConfig.endDate,
            conversionEvents = JSONArray(testConfig.conversionEvents).toString(),
            metadata = createTestMetadata(testConfig)
        )
        
        analyticsDao.insertABTest(abTest)
        return@withContext variant
    }
    
    /**
     * Record A/B test impression
     */
    suspend fun recordImpression(userId: String, testName: String) = withContext(Dispatchers.IO) {
        val abTest = analyticsDao.getActiveABTest(userId, testName)
        if (abTest != null) {
            analyticsDao.recordABTestImpression(abTest.id, System.currentTimeMillis())
        }
    }
    
    /**
     * Record A/B test click/interaction
     */
    suspend fun recordClick(userId: String, testName: String) = withContext(Dispatchers.IO) {
        val abTest = analyticsDao.getActiveABTest(userId, testName)
        if (abTest != null) {
            analyticsDao.recordABTestClick(abTest.id, System.currentTimeMillis())
        }
    }
    
    /**
     * Record A/B test conversion event
     */
    suspend fun recordConversion(userId: String, testName: String, eventType: String) = withContext(Dispatchers.IO) {
        val abTest = analyticsDao.getActiveABTest(userId, testName)
        if (abTest != null) {
            val testConfig = ACTIVE_TESTS[testName]
            if (testConfig?.conversionEvents?.contains(eventType) == true) {
                analyticsDao.recordABTestConversion(abTest.id, System.currentTimeMillis())
            }
        }
    }
    
    /**
     * Record engagement time for A/B test
     */
    suspend fun recordEngagementTime(userId: String, testName: String, duration: Long) = withContext(Dispatchers.IO) {
        val abTest = analyticsDao.getActiveABTest(userId, testName)
        if (abTest != null) {
            analyticsDao.updateABTestEngagementTime(abTest.id, duration, System.currentTimeMillis())
        }
    }
    
    /**
     * Get all active A/B tests for a user
     */
    suspend fun getActiveTests(userId: String): List<ABTestEntity> = withContext(Dispatchers.IO) {
        analyticsDao.getActiveABTests(userId)
    }
    
    /**
     * Get A/B test results and performance metrics
     */
    suspend fun getTestResults(testName: String): ABTestResults? = withContext(Dispatchers.IO) {
        val testConfig = ACTIVE_TESTS[testName] ?: return@withContext null
        
        // This would typically query all users in the test, but for simplicity
        // we'll return a placeholder structure
        ABTestResults(
            testName = testName,
            variants = testConfig.variants,
            startDate = testConfig.startDate,
            endDate = testConfig.endDate,
            isActive = System.currentTimeMillis() in testConfig.startDate..testConfig.endDate,
            variantResults = testConfig.variants.map { variant ->
                VariantResult(
                    variant = variant,
                    impressions = 0, // Would be calculated from database
                    clicks = 0,
                    conversions = 0,
                    conversionRate = 0.0,
                    averageEngagementTime = 0L,
                    statisticalSignificance = 0.0
                )
            }
        )
    }
    
    /**
     * Deactivate expired A/B tests
     */
    suspend fun deactivateExpiredTests() = withContext(Dispatchers.IO) {
        analyticsDao.deactivateExpiredABTests(System.currentTimeMillis())
    }
    
    /**
     * Assign user to variant based on traffic allocation and consistent hashing
     */
    private fun assignUserToVariant(userId: String, testConfig: ABTestConfig): String {
        // Use consistent hashing to ensure same user always gets same variant
        val hash = (userId + testConfig.name).hashCode()
        val normalizedHash = (hash.toDouble() / Int.MAX_VALUE + 1.0) / 2.0 // Normalize to 0-1
        
        var cumulativeProbability = 0.0
        for ((variant, allocation) in testConfig.trafficAllocation) {
            cumulativeProbability += allocation
            if (normalizedHash <= cumulativeProbability) {
                return variant
            }
        }
        
        // Fallback to first variant
        return testConfig.variants.first()
    }
    
    /**
     * Create test metadata JSON
     */
    private fun createTestMetadata(testConfig: ABTestConfig): String {
        return JSONObject().apply {
            put("traffic_allocation", JSONObject(testConfig.trafficAllocation))
            put("conversion_events", JSONArray(testConfig.conversionEvents))
            put("description", getTestDescription(testConfig.name))
        }.toString()
    }
    
    /**
     * Get human-readable test description
     */
    private fun getTestDescription(testName: String): String {
        return when (testName) {
            "content_layout_v1" -> "Testing different content layout designs for better engagement"
            "recommendation_algorithm_v2" -> "Comparing recommendation algorithms for personalized content"
            "reading_progress_indicator" -> "Testing different ways to show reading progress"
            else -> "A/B test for content optimization"
        }
    }
    
    /**
     * Check if user should see a specific feature based on A/B test
     */
    suspend fun shouldShowFeature(userId: String, testName: String, requiredVariant: String): Boolean {
        val variant = getTestVariant(userId, testName)
        return variant == requiredVariant
    }
    
    /**
     * Get recommended content layout based on A/B test
     */
    suspend fun getContentLayoutVariant(userId: String): String {
        return getTestVariant(userId, "content_layout_v1") ?: "card_layout"
    }
    
    /**
     * Get recommendation algorithm variant
     */
    suspend fun getRecommendationAlgorithmVariant(userId: String): String {
        return getTestVariant(userId, "recommendation_algorithm_v2") ?: "hybrid"
    }
    
    /**
     * Get reading progress indicator variant
     */
    suspend fun getReadingProgressVariant(userId: String): String {
        return getTestVariant(userId, "reading_progress_indicator") ?: "control"
    }
}

/**
 * A/B Test configuration data class
 */
data class ABTestConfig(
    val name: String,
    val variants: List<String>,
    val trafficAllocation: Map<String, Float>,
    val conversionEvents: List<String>,
    val startDate: Long,
    val endDate: Long
)

/**
 * A/B Test results data class
 */
data class ABTestResults(
    val testName: String,
    val variants: List<String>,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val variantResults: List<VariantResult>
)

/**
 * Individual variant results
 */
data class VariantResult(
    val variant: String,
    val impressions: Int,
    val clicks: Int,
    val conversions: Int,
    val conversionRate: Double,
    val averageEngagementTime: Long,
    val statisticalSignificance: Double
)