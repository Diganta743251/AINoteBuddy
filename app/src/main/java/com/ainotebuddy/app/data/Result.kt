package com.ainotebuddy.app.data

/**
 * Generic Result wrapper for handling success and error states
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    val isLoading: Boolean
        get() = this is Loading
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }
    
    inline fun onSuccess(action: (@UnsafeVariance T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String) -> Unit): Result<T> {
        if (this is Error) action(message)
        return this
    }
}

/**
 * Convenience functions for creating Result instances
 */
fun <T> T.asSuccess(): Result<T> = Result.Success(this)
fun String.asError(): Result<Nothing> = Result.Error(this)
fun loading(): Result<Nothing> = Result.Loading