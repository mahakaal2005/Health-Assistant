package com.example.health_assistant.features.discover.domain.repository

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverSections

/**
 * Simplified repository interface for basic discover content fetching
 */
interface DiscoverRepository {
    
    /**
     * Get all discover content organized by sections
     */
    suspend fun getDiscoverContent(): Result<DiscoverSections>
    
    /**
     * Refresh content from remote APIs
     */
    suspend fun refreshContent(): Result<DiscoverSections>
}