package com.example.health_assistant.features.journal.data

import androidx.room.*
import com.example.health_assistant.features.journal.domain.ActivityCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActivityCardDao {
    @Query("SELECT * FROM activity_cards ORDER BY date DESC")
    fun getAllActivityCards(): Flow<List<ActivityCard>>

    @Query("SELECT * FROM activity_cards WHERE userId = :userId ORDER BY date DESC")
    fun getAllActivityCardsByUserId(userId: String): Flow<List<ActivityCard>>

    @Query("SELECT * FROM activity_cards WHERE date = :date LIMIT 1")
    suspend fun getActivityCardByDate(date: LocalDate): ActivityCard?

    @Query("SELECT * FROM activity_cards WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getActivityCardByDateAndUserId(date: LocalDate, userId: String): ActivityCard?

    @Query("SELECT * FROM activity_cards WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>>

    @Query("SELECT * FROM activity_cards WHERE date BETWEEN :startDate AND :endDate AND userId = :userId ORDER BY date DESC")
    fun getActivityCardsByDateRangeAndUserId(startDate: LocalDate, endDate: LocalDate, userId: String): Flow<List<ActivityCard>>

    @Query("SELECT * FROM activity_cards ORDER BY date DESC LIMIT :limit")
    fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>>

    @Query("SELECT * FROM activity_cards WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentActivityCardsByUserId(userId: String, limit: Int): Flow<List<ActivityCard>>

    @Query("SELECT EXISTS(SELECT 1 FROM activity_cards WHERE date = :date)")
    suspend fun activityCardExistsForDate(date: LocalDate): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM activity_cards WHERE date = :date AND userId = :userId)")
    suspend fun activityCardExistsForDateAndUserId(date: LocalDate, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityCard(activityCard: ActivityCard): Long

    @Query("SELECT COUNT(*) FROM activity_cards")
    suspend fun getActivityCardCount(): Int

    @Query("SELECT COUNT(*) FROM activity_cards WHERE userId = :userId")
    suspend fun getActivityCardCountByUserId(userId: String): Int

    @Update
    suspend fun updateActivityCard(activityCard: ActivityCard)

    @Delete
    suspend fun deleteActivityCard(activityCard: ActivityCard)
}