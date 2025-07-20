package com.example.health_assistant.features.discover.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for News API responses
 */
data class NewsApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("articles") val articles: List<NewsApiArticle>
)

data class NewsApiArticle(
    @SerializedName("source") val source: NewsApiSource,
    @SerializedName("author") val author: String?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("content") val content: String?
)

data class NewsApiSource(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String
)

/**
 * Guardian API Response DTOs
 */
data class GuardianApiResponse(
    @SerializedName("response") val response: GuardianResponse
)

data class GuardianResponse(
    @SerializedName("status") val status: String,
    @SerializedName("userTier") val userTier: String,
    @SerializedName("total") val total: Int,
    @SerializedName("results") val results: List<GuardianArticle>
)

data class GuardianArticle(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("sectionId") val sectionId: String,
    @SerializedName("sectionName") val sectionName: String,
    @SerializedName("webPublicationDate") val webPublicationDate: String,
    @SerializedName("webTitle") val webTitle: String,
    @SerializedName("webUrl") val webUrl: String,
    @SerializedName("apiUrl") val apiUrl: String,
    @SerializedName("fields") val fields: GuardianFields?
)

data class GuardianFields(
    @SerializedName("headline") val headline: String?,
    @SerializedName("standfirst") val standfirst: String?,
    @SerializedName("trailText") val trailText: String?,
    @SerializedName("byline") val byline: String?,
    @SerializedName("main") val main: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("thumbnail") val thumbnail: String?
)

/**
 * YouTube API Response DTOs
 */
data class YouTubeSearchResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("regionCode") val regionCode: String?,
    @SerializedName("pageInfo") val pageInfo: YouTubePageInfo,
    @SerializedName("items") val items: List<YouTubeVideo>
)

data class YouTubePageInfo(
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("resultsPerPage") val resultsPerPage: Int
)

data class YouTubeVideo(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("id") val id: YouTubeVideoId,
    @SerializedName("snippet") val snippet: YouTubeSnippet
)

data class YouTubeVideoId(
    @SerializedName("kind") val kind: String,
    @SerializedName("videoId") val videoId: String
)

data class YouTubeSnippet(
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("channelId") val channelId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnails,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("liveBroadcastContent") val liveBroadcastContent: String
)

data class YouTubeThumbnails(
    @SerializedName("default") val default: YouTubeThumbnail?,
    @SerializedName("medium") val medium: YouTubeThumbnail?,
    @SerializedName("high") val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

/**
 * Reddit API Response DTOs
 */
data class RedditResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("data") val data: RedditData
)

data class RedditData(
    @SerializedName("modhash") val modhash: String?,
    @SerializedName("dist") val dist: Int?,
    @SerializedName("children") val children: List<RedditPost>,
    @SerializedName("after") val after: String?,
    @SerializedName("before") val before: String?
)

data class RedditPost(
    @SerializedName("kind") val kind: String,
    @SerializedName("data") val data: RedditPostData
)

data class RedditPostData(
    @SerializedName("subreddit") val subreddit: String,
    @SerializedName("selftext") val selftext: String?,
    @SerializedName("author_fullname") val authorFullname: String?,
    @SerializedName("title") val title: String,
    @SerializedName("subreddit_name_prefixed") val subredditNamePrefixed: String,
    @SerializedName("name") val name: String,
    @SerializedName("upvote_ratio") val upvoteRatio: Double?,
    @SerializedName("ups") val ups: Int,
    @SerializedName("created_utc") val createdUtc: Double,
    @SerializedName("url") val url: String,
    @SerializedName("author") val author: String,
    @SerializedName("num_comments") val numComments: Int,
    @SerializedName("permalink") val permalink: String,
    @SerializedName("score") val score: Int,
    @SerializedName("is_self") val isSelf: Boolean,
    @SerializedName("domain") val domain: String?
)