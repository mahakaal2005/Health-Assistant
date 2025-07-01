package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.model.DiseaseCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class,
        ProfileImageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao
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
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback to initialize default data
         * Made internal so it can be accessed from DatabaseModule
         */
        internal class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // Initialize default categories when database is first created
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.diseaseCategoryDao())
                    }
                }
            }
        }

        /**
         * Populate the database with default disease categories
         * This prevents foreign key constraint errors
         */
        private suspend fun populateDefaultCategories(diseaseCategoryDao: DiseaseCategoryDao) {
            try {
                val defaultCategories = DiseaseCategory.getDefaultCategories()
                val entities = defaultCategories.map { it.toEntity() }
                diseaseCategoryDao.insertCategories(entities)
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.e("Database", "Error populating default categories", e)
            }
        }
    }
}