package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import com.example.health_assistant.features.journal.data.ActivityCardDao
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.discover.data.entity.HealthArticleEntity
import com.example.health_assistant.features.discover.data.entity.HealthNewsEntity
import com.example.health_assistant.features.discover.data.entity.EducationalVideoEntity
import com.example.health_assistant.features.discover.data.entity.ContentBookmarkEntity
import com.example.health_assistant.features.discover.data.entity.ContentAnalyticsEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.data.entity.ABTestEntity
import com.example.health_assistant.features.discover.data.DiscoverDao
import com.example.health_assistant.features.discover.data.AnalyticsDao

/**
 * Main database for the Health Assistant application
 * Integrates all entities: prescriptions, profile images, disease categories, journal entries, and activity cards
 */
@Database(
    entities = [
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class,
        JournalEntryEntity::class,
        ProfileImageEntity::class,
        ActivityCard::class,
        HealthArticleEntity::class,
        HealthNewsEntity::class,
        EducationalVideoEntity::class,
        ContentBookmarkEntity::class,
        ContentAnalyticsEntity::class,
        UserEngagementEntity::class,
        ContentRecommendationEntity::class,
        ABTestEntity::class
    ],
    version = 7, // Increment to include Analytics entities
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun profileImageDao(): ProfileImageDao
    abstract fun activityCardDao(): ActivityCardDao
    abstract fun discoverDao(): DiscoverDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        const val DATABASE_NAME = "health_assistant_database"

        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        /**
         * Migration from version 4 to 5:
         * - Add userId column to prescriptions table
         * - Add userId column to journal_entries table
         * - Add userId column to activity_cards table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userId column to prescriptions table
                database.execSQL("ALTER TABLE prescriptions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_prescriptions_userId ON prescriptions(userId)")

                // Add userId column to journal_entries table
                database.execSQL("ALTER TABLE journal_entries ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_journal_entries_userId ON journal_entries(userId)")
                database.execSQL("CREATE INDEX index_journal_entries_type ON journal_entries(type)")

                // Add userId column to activity_cards table
                database.execSQL("ALTER TABLE activity_cards ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_activity_cards_userId ON activity_cards(userId)")
                database.execSQL("CREATE INDEX index_activity_cards_date ON activity_cards(date)")
            }
        }

        /**
         * Migration from version 5 to 6:
         * - Create health_articles table for Discover feature
         * - Create health_news table for Discover feature
         * - Create educational_videos table for Discover feature
         * - Create content_bookmarks table for Discover feature
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create health_articles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS health_articles (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL,
                        authorName TEXT NOT NULL,
                        authorCredentials TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        publishedDate INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        readingTimeMinutes INTEGER NOT NULL,
                        imageUrl TEXT,
                        tags TEXT NOT NULL,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        readProgress REAL NOT NULL DEFAULT 0.0,
                        credibilityScore INTEGER NOT NULL,
                        userId TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // Create indices for health_articles
                database.execSQL("CREATE INDEX index_health_articles_userId ON health_articles(userId)")
                database.execSQL("CREATE INDEX index_health_articles_category ON health_articles(category)")
                database.execSQL("CREATE INDEX index_health_articles_publishedDate ON health_articles(publishedDate)")
                database.execSQL("CREATE INDEX index_health_articles_credibilityScore ON health_articles(credibilityScore)")
                database.execSQL("CREATE INDEX index_health_articles_userId_category ON health_articles(userId, category)")
                database.execSQL("CREATE INDEX index_health_articles_userId_isBookmarked ON health_articles(userId, isBookmarked)")

                // Create health_news table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS health_news (
                        id TEXT NOT NULL PRIMARY KEY,
                        headline TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        fullContent TEXT,
                        category TEXT NOT NULL,
                        sourcePublication TEXT NOT NULL,
                        sourceCredibility TEXT NOT NULL,
                        publishedDate INTEGER NOT NULL,
                        imageUrl TEXT,
                        externalUrl TEXT NOT NULL,
                        isBreakingNews INTEGER NOT NULL DEFAULT 0,
                        relevanceScore INTEGER NOT NULL,
                        userId TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // Create indices for health_news
                database.execSQL("CREATE INDEX index_health_news_userId ON health_news(userId)")
                database.execSQL("CREATE INDEX index_health_news_category ON health_news(category)")
                database.execSQL("CREATE INDEX index_health_news_publishedDate ON health_news(publishedDate)")
                database.execSQL("CREATE INDEX index_health_news_sourceCredibility ON health_news(sourceCredibility)")
                database.execSQL("CREATE INDEX index_health_news_isBreakingNews ON health_news(isBreakingNews)")
                database.execSQL("CREATE INDEX index_health_news_userId_category ON health_news(userId, category)")
                database.execSQL("CREATE INDEX index_health_news_userId_isBreakingNews ON health_news(userId, isBreakingNews)")

                // Create educational_videos table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS educational_videos (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        videoUrl TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        difficultyLevel TEXT NOT NULL,
                        expertName TEXT NOT NULL,
                        expertCredentials TEXT NOT NULL,
                        publishedDate INTEGER NOT NULL,
                        watchProgress REAL NOT NULL DEFAULT 0.0,
                        isDownloadedOffline INTEGER NOT NULL DEFAULT 0,
                        transcriptAvailable INTEGER NOT NULL DEFAULT 0,
                        userId TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // Create indices for educational_videos
                database.execSQL("CREATE INDEX index_educational_videos_userId ON educational_videos(userId)")
                database.execSQL("CREATE INDEX index_educational_videos_category ON educational_videos(category)")
                database.execSQL("CREATE INDEX index_educational_videos_publishedDate ON educational_videos(publishedDate)")
                database.execSQL("CREATE INDEX index_educational_videos_difficultyLevel ON educational_videos(difficultyLevel)")
                database.execSQL("CREATE INDEX index_educational_videos_isDownloadedOffline ON educational_videos(isDownloadedOffline)")
                database.execSQL("CREATE INDEX index_educational_videos_userId_category ON educational_videos(userId, category)")
                database.execSQL("CREATE INDEX index_educational_videos_userId_isDownloadedOffline ON educational_videos(userId, isDownloadedOffline)")

                // Create content_bookmarks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS content_bookmarks (
                        id TEXT NOT NULL PRIMARY KEY,
                        contentId TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        bookmarkedDate INTEGER NOT NULL,
                        userId TEXT NOT NULL
                    )
                """.trimIndent())

                // Create indices for content_bookmarks
                database.execSQL("CREATE INDEX index_content_bookmarks_userId ON content_bookmarks(userId)")
                database.execSQL("CREATE INDEX index_content_bookmarks_contentId ON content_bookmarks(contentId)")
                database.execSQL("CREATE INDEX index_content_bookmarks_contentType ON content_bookmarks(contentType)")
                database.execSQL("CREATE INDEX index_content_bookmarks_bookmarkedDate ON content_bookmarks(bookmarkedDate)")
                database.execSQL("CREATE INDEX index_content_bookmarks_userId_contentType ON content_bookmarks(userId, contentType)")
                database.execSQL("CREATE INDEX index_content_bookmarks_userId_bookmarkedDate ON content_bookmarks(userId, bookmarkedDate)")
            }
        }

        /**
         * Migration from version 6 to 7:
         * - Create content_analytics table for user engagement tracking
         * - Create user_engagement table for engagement patterns
         * - Create content_recommendations table for personalized recommendations
         * - Create ab_tests table for A/B testing framework
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create content_analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS content_analytics (
                        id TEXT NOT NULL PRIMARY KEY,
                        contentId TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        sessionId TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        duration INTEGER NOT NULL DEFAULT 0,
                        progress REAL NOT NULL DEFAULT 0.0,
                        metadata TEXT NOT NULL DEFAULT '{}',
                        category TEXT NOT NULL DEFAULT '',
                        source TEXT NOT NULL DEFAULT '',
                        deviceType TEXT NOT NULL DEFAULT '',
                        networkType TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // Create indices for content_analytics
                database.execSQL("CREATE INDEX index_content_analytics_contentId ON content_analytics(contentId)")
                database.execSQL("CREATE INDEX index_content_analytics_contentType ON content_analytics(contentType)")
                database.execSQL("CREATE INDEX index_content_analytics_userId ON content_analytics(userId)")
                database.execSQL("CREATE INDEX index_content_analytics_sessionId ON content_analytics(sessionId)")
                database.execSQL("CREATE INDEX index_content_analytics_timestamp ON content_analytics(timestamp)")
                database.execSQL("CREATE INDEX index_content_analytics_eventType ON content_analytics(eventType)")
                database.execSQL("CREATE INDEX index_content_analytics_userId_contentId ON content_analytics(userId, contentId)")
                database.execSQL("CREATE INDEX index_content_analytics_userId_eventType ON content_analytics(userId, eventType)")
                database.execSQL("CREATE INDEX index_content_analytics_contentId_eventType ON content_analytics(contentId, eventType)")

                // Create user_engagement table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_engagement (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        category TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        totalViews INTEGER NOT NULL DEFAULT 0,
                        totalReadingTime INTEGER NOT NULL DEFAULT 0,
                        averageReadingTime INTEGER NOT NULL DEFAULT 0,
                        completionRate REAL NOT NULL DEFAULT 0.0,
                        bookmarkRate REAL NOT NULL DEFAULT 0.0,
                        shareRate REAL NOT NULL DEFAULT 0.0,
                        engagementScore REAL NOT NULL DEFAULT 0.0,
                        preferenceWeight REAL NOT NULL DEFAULT 0.0,
                        lastEngagement INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indices for user_engagement
                database.execSQL("CREATE INDEX index_user_engagement_userId ON user_engagement(userId)")
                database.execSQL("CREATE INDEX index_user_engagement_category ON user_engagement(category)")
                database.execSQL("CREATE INDEX index_user_engagement_contentType ON user_engagement(contentType)")
                database.execSQL("CREATE INDEX index_user_engagement_lastUpdated ON user_engagement(lastUpdated)")
                database.execSQL("CREATE INDEX index_user_engagement_userId_category ON user_engagement(userId, category)")
                database.execSQL("CREATE INDEX index_user_engagement_userId_contentType ON user_engagement(userId, contentType)")

                // Create content_recommendations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS content_recommendations (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        contentId TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        recommendationType TEXT NOT NULL,
                        score REAL NOT NULL,
                        reason TEXT NOT NULL,
                        algorithmVersion TEXT NOT NULL,
                        category TEXT NOT NULL,
                        tags TEXT NOT NULL DEFAULT '[]',
                        metadata TEXT NOT NULL DEFAULT '{}',
                        isShown INTEGER NOT NULL DEFAULT 0,
                        isClicked INTEGER NOT NULL DEFAULT 0,
                        isBookmarked INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create indices for content_recommendations
                database.execSQL("CREATE INDEX index_content_recommendations_userId ON content_recommendations(userId)")
                database.execSQL("CREATE INDEX index_content_recommendations_contentId ON content_recommendations(contentId)")
                database.execSQL("CREATE INDEX index_content_recommendations_recommendationType ON content_recommendations(recommendationType)")
                database.execSQL("CREATE INDEX index_content_recommendations_score ON content_recommendations(score)")
                database.execSQL("CREATE INDEX index_content_recommendations_createdAt ON content_recommendations(createdAt)")
                database.execSQL("CREATE INDEX index_content_recommendations_userId_recommendationType ON content_recommendations(userId, recommendationType)")
                database.execSQL("CREATE INDEX index_content_recommendations_userId_score ON content_recommendations(userId, score)")

                // Create ab_tests table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ab_tests (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        testName TEXT NOT NULL,
                        variant TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        assignedAt INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        conversionEvents TEXT NOT NULL DEFAULT '[]',
                        metadata TEXT NOT NULL DEFAULT '{}',
                        impressions INTEGER NOT NULL DEFAULT 0,
                        clicks INTEGER NOT NULL DEFAULT 0,
                        conversions INTEGER NOT NULL DEFAULT 0,
                        engagementTime INTEGER NOT NULL DEFAULT 0,
                        lastInteraction INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create indices for ab_tests
                database.execSQL("CREATE INDEX index_ab_tests_userId ON ab_tests(userId)")
                database.execSQL("CREATE INDEX index_ab_tests_testName ON ab_tests(testName)")
                database.execSQL("CREATE INDEX index_ab_tests_variant ON ab_tests(variant)")
                database.execSQL("CREATE INDEX index_ab_tests_isActive ON ab_tests(isActive)")
                database.execSQL("CREATE INDEX index_ab_tests_startDate ON ab_tests(startDate)")
                database.execSQL("CREATE INDEX index_ab_tests_userId_testName ON ab_tests(userId, testName)")
                database.execSQL("CREATE INDEX index_ab_tests_testName_variant ON ab_tests(testName, variant)")
            }
        }

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // Allow schema recreation on conflicts
                .fallbackToDestructiveMigrationOnDowngrade() // Handle version downgrades
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7) // Add migrations from version 4 to 5, 5 to 6, and 6 to 7
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes - creates an in-memory database
         */
        fun getInMemoryDatabase(context: Context): HealthAssistantDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                HealthAssistantDatabase::class.java
            ).build()
        }

        /**
         * Force close and reset database instance
         * Call this to clear cached schemas and force recreation
         */
        fun resetDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}