package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity

/**
 * Room Database for local data storage
 * Currently manages profile images with plans for future expansion
 */
@Database(
    entities = [ProfileImageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun profileImageDao(): ProfileImageDao

    companion object {
        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    "health_assistant_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}