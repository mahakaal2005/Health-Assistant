package com.example.health_assistant.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Module previously contained a redundant SessionManager provider that caused a dependency cycle.
 * Since SessionManager already has @Inject constructor and @Singleton annotations,
 * it doesn't need an explicit provider.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule