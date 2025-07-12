package com.example.health_assistant.di

import android.content.Context
import com.example.health_assistant.data.sensors.DeviceSensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for step tracking dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object StepTrackingModule {

    @Provides
    @Singleton
    fun provideDeviceSensorManager(
        @ApplicationContext context: Context
    ): DeviceSensorManager {
        return DeviceSensorManager(context)
    }
}