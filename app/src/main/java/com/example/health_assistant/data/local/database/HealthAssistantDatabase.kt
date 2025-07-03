package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import com.example.health_assistant.data.model.PrescriptionEntity
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalEntryEntity

@Database(
    entities = [
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class,
        JournalEntryEntity::class,
        ProfileImageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun profileImageDao(): ProfileImageDao

    companion object {
        const val DATABASE_NAME = "health_assistant_database"

        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}