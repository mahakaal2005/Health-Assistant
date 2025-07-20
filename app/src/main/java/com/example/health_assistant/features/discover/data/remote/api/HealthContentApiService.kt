package com.example.health_assistant.features.discover.data.remote.api

import com.example.health_assistant.features.discover.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

/**
 * Retrofit API service interfaces for health content sources
 */
interface NewsApiService {
    @GET("everything")
    suspend fun searchHealthNews(
        @Query("q") query: String,
        @Query("domains") domains: String? = "mayoclinic.org,webmd.com,healthline.com,medicalnewstoday.com",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("apiKey") apiKey: String
    ): Response<NewsApiResponse>
    
    @GET("top-headlines")
    suspend fun getHealthTopHeadlines(
        @Query("category") category: String = "health",
        @Query("country") country: String = "us",
        @Query("pageSize") pageSize: Int = 20,
        @Query("apiKey") apiKey: String
    ): Response<NewsApiResponse>
}

interface GuardianApiService {
    @GET("search")
    suspend fun searchHealthArticles(
        @Query("q") query: String? = null,
        @Query("section") section: String? = "society/health",
        @Query("show-fields") showFields: String = "headline,standfirst,trailText,byline,main,body,thumbnail",
        @Query("page-size") pageSize: Int = 20,
        @Query("order-by") orderBy: String = "newest",
        @Query("api-key") apiKey: String
    ): Response<GuardianApiResponse>
}

interface YouTubeApiService {
    @GET("search")
    suspend fun searchHealthVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoDuration") videoDuration: String = "medium", // 4-20 minutes
        @Query("videoDefinition") videoDefinition: String = "high",
        @Query("maxResults") maxResults: Int = 25,
        @Query("order") order: String = "relevance",
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>
    
    @GET("search")
    suspend fun getChannelVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 10,
        @Query("order") order: String = "date",
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>
}

interface RedditApiService {
    @GET("r/{subreddit}/hot.json")
    suspend fun getSubredditPosts(
        @Path("subreddit") subreddit: String,
        @Query("limit") limit: Int = 25,
        @Query("t") timeframe: String = "week" // week, month, year, all
    ): Response<RedditResponse>
    
    @GET("r/{subreddit}/top.json")
    suspend fun getTopSubredditPosts(
        @Path("subreddit") subreddit: String,
        @Query("limit") limit: Int = 25,
        @Query("t") timeframe: String = "week"
    ): Response<RedditResponse>
}