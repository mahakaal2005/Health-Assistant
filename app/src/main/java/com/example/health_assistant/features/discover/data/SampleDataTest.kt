package com.example.health_assistant.features.discover.data

import android.util.Log
import com.example.health_assistant.core.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple test class to verify sample data is working
 * This can be called from the app to test the repository
 */
@Singleton
class SampleDataTest @Inject constructor(
    private val repository: DiscoverRepositoryImpl
) {
    
    fun testSampleData() {
        runBlocking {
            try {
                Log.d("SampleDataTest", "Testing sample data...")
                
                // Test articles
                val articles = repository.getHealthArticles(null, 5).first()
                Log.d("SampleDataTest", "Articles result: $articles")
                
                // Test news
                val news = repository.getHealthNews(null, 3).first()
                Log.d("SampleDataTest", "News result: $news")
                
                // Test videos
                val videos = repository.getEducationalVideos(null, 3).first()
                Log.d("SampleDataTest", "Videos result: $videos")
                
                // Test mixed content
                val mixed = repository.getMixedContentFeed(null, 10).first()
                Log.d("SampleDataTest", "Mixed content result: $mixed")
                
                when (mixed) {
                    is Result.Success -> {
                        Log.d("SampleDataTest", "SUCCESS: Got ${mixed.data.size} items")
                        mixed.data.forEach { content ->
                            Log.d("SampleDataTest", "- ${content.getContentType()}: ${content.title}")
                        }
                    }
                    is Result.Error -> {
                        Log.e("SampleDataTest", "ERROR: ${mixed.exception?.message}")
                    }
                    is Result.Loading -> {
                        Log.d("SampleDataTest", "LOADING...")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("SampleDataTest", "Exception during test", e)
            }
        }
    }
}