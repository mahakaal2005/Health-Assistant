package com.example.health_assistant.core.util

/**
 * A sealed class representing different states of data loading operations.
 * Used throughout the app to provide consistent error handling and loading states.
 */
sealed class Result<out T> {
    /**
     * Represents a loading state - operation is in progress
     */
    object Loading : Result<Nothing>()

    /**
     * Represents a successful operation with data
     * @param data The successfully loaded data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents an error state with optional error details
     * @param exception The exception that caused the error
     * @param message Human-readable error message
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String = exception?.message ?: "Unknown error occurred"
    ) : Result<Nothing>()

    /**
     * Returns true if this is a Success result
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an Error result
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this is a Loading result
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if Success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}


/**
 * Extension function for async operations that return Result
 */
suspend inline fun <T> safeSuspendCall(crossinline action: suspend () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(exception = e)
    }
}

/**
 * Execute block if result is Success
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Execute block if result is Error
 */
inline fun <T> Result<T>.onFailure(action: (Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}