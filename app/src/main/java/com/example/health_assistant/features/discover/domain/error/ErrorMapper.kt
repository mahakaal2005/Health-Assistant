package com.example.health_assistant.features.discover.domain.error

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps various exceptions to DiscoverError instances with appropriate user messages
 */
@Singleton
class ErrorMapper @Inject constructor() {
    
    fun mapThrowableToDiscoverError(throwable: Throwable): DiscoverError {
        return when (throwable) {
            // Network errors
            is UnknownHostException -> DiscoverError.NetworkError(
                errorMessage = "No internet connection",
                throwable = throwable
            )
            
            is SocketTimeoutException -> DiscoverError.TimeoutError(
                errorMessage = "Connection timed out",
                throwable = throwable
            )
            
            is IOException -> DiscoverError.NetworkError(
                errorMessage = "Network I/O error: ${throwable.message}",
                throwable = throwable
            )
            
            // Firebase errors
            is FirebaseNetworkException -> DiscoverError.NetworkError(
                errorMessage = "Firebase network error",
                throwable = throwable
            )
            
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE -> DiscoverError.ServerError(
                        errorMessage = "Firebase service unavailable",
                        throwable = throwable
                    )
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DiscoverError.TimeoutError(
                        errorMessage = "Firebase request timeout",
                        throwable = throwable
                    )
                    FirebaseFirestoreException.Code.NOT_FOUND -> DiscoverError.ContentNotFoundError(
                        contentId = "unknown",
                        contentType = "firebase_document"
                    )
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> DiscoverError.ServerError(
                        errorMessage = "Firebase permission denied",
                        throwable = throwable
                    )
                    else -> DiscoverError.ServerError(
                        errorMessage = "Firebase error: ${throwable.message}",
                        throwable = throwable
                    )
                }
            }
            
            is FirebaseException -> DiscoverError.ServerError(
                errorMessage = "Firebase error: ${throwable.message}",
                throwable = throwable
            )
            
            // Database errors
            is android.database.SQLException -> DiscoverError.DatabaseError(
                errorMessage = "Database error: ${throwable.message}",
                throwable = throwable
            )
            
            // Generic errors
            else -> DiscoverError.UnknownError(
                errorMessage = throwable.message ?: "Unknown error occurred",
                throwable = throwable
            )
        }
    }
    
    fun mapPartialLoadFailure(
        loadedCount: Int,
        totalCount: Int,
        failedOperations: Map<String, Throwable>
    ): DiscoverError.PartialContentError {
        return DiscoverError.PartialContentError(
            loadedCount = loadedCount,
            totalCount = totalCount,
            failedTypes = failedOperations.keys.toList()
        )
    }
}