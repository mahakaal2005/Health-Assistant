package com.example.health_assistant.features.discover.domain.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {
    
    private lateinit var errorMapper: ErrorMapper
    
    @Before
    fun setUp() {
        errorMapper = ErrorMapper()
    }
    
    @Test
    fun `mapThrowableToDiscoverError maps UnknownHostException to NetworkError`() {
        // Given
        val exception = UnknownHostException("No internet connection")
        
        // When
        val result = errorMapper.mapThrowableToDiscoverError(exception)
        
        // Then
        assertTrue(result is DiscoverError.NetworkError)
        assertEquals("No internet connection", result.message)
        assertEquals("Unable to connect to the internet. Please check your connection and try again.", result.userMessage)
        assertTrue(result.isRetryable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `mapThrowableToDiscoverError maps SocketTimeoutException to TimeoutError`() {
        // Given
        val exception = SocketTimeoutException("Connection timed out")
        
        // When
        val result = errorMapper.mapThrowableToDiscoverError(exception)
        
        // Then
        assertTrue(result is DiscoverError.TimeoutError)
        assertEquals("Connection timed out", result.message)
        assertEquals("The request is taking too long. Please try again.", result.userMessage)
        assertTrue(result.isRetryable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `mapThrowableToDiscoverError maps IOException to NetworkError`() {
        // Given
        val exception = IOException("Network I/O error")
        
        // When
        val result = errorMapper.mapThrowableToDiscoverError(exception)
        
        // Then
        assertTrue(result is DiscoverError.NetworkError)
        assertEquals("Network I/O error: Network I/O error", result.message)
        assertEquals("Unable to connect to the internet. Please check your connection and try again.", result.userMessage)
        assertTrue(result.isRetryable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `mapThrowableToDiscoverError maps FirebaseNetworkException to NetworkError`() {
        // Given
        val exception = FirebaseNetworkException("Firebase network error")
        
        // When
        val result = errorMapper.mapThrowableToDiscoverError(exception)
        
        // Then
        assertTrue(result is DiscoverError.NetworkError)
        assertEquals("Firebase network error", result.message)
        assertEquals("Unable to connect to the internet. Please check your connection and try again.", result.userMessage)
        assertTrue(result.isRetryable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `mapThrowableToDiscoverError maps unknown exception to UnknownError`() {
        // Given
        val exception = RuntimeException("Unknown error")
        
        // When
        val result = errorMapper.mapThrowableToDiscoverError(exception)
        
        // Then
        assertTrue(result is DiscoverError.UnknownError)
        assertEquals("Unknown error", result.message)
        assertEquals("Something went wrong. Please try again.", result.userMessage)
        assertTrue(result.isRetryable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `mapPartialLoadFailure creates PartialContentError with correct details`() {
        // Given
        val loadedCount = 5
        val totalCount = 10
        val failedOperations = mapOf(
            "articles" to IOException("Network error"),
            "videos" to SocketTimeoutException("Timeout")
        )
        
        // When
        val result = errorMapper.mapPartialLoadFailure(loadedCount, totalCount, failedOperations)
        
        // Then
        assertEquals(loadedCount, result.loadedCount)
        assertEquals(totalCount, result.totalCount)
        assertEquals(listOf("articles", "videos"), result.failedTypes)
        assertEquals("Partial content load: 5/10 loaded, failed: [articles, videos]", result.message)
        assertEquals("Some content couldn't be loaded. Showing available content.", result.userMessage)
        assertTrue(result.isRetryable)
    }
}