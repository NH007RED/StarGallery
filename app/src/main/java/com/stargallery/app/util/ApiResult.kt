package com.stargallery.app.util

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** What went wrong, expressed without ever including URLs or credentials. */
sealed interface AppError {
    data object NotConfigured : AppError
    data object Network : AppError
    data object Timeout : AppError
    data object Unauthorized : AppError
    data object NotFound : AppError
    data object RateLimited : AppError
    data class Server(val code: Int) : AppError
    data class Http(val code: Int) : AppError
    data object Unknown : AppError

    fun message(): String = when (this) {
        NotConfigured -> "TMDB credentials have not been configured."
        Network -> "No connection. Check your internet and try again."
        Timeout -> "The request timed out. Please try again."
        Unauthorized -> "TMDB rejected the credentials. Check your API key or token."
        NotFound -> "This item could not be found."
        RateLimited -> "Too many requests. Please wait a moment and retry."
        is Server -> "TMDB is having trouble right now (HTTP $code). Try again later."
        is Http -> "Request failed (HTTP $code)."
        Unknown -> "Something went wrong. Please try again."
    }
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Failure(
        when (val code = e.code()) {
            401, 403 -> AppError.Unauthorized
            404 -> AppError.NotFound
            429 -> AppError.RateLimited
            in 500..599 -> AppError.Server(code)
            else -> AppError.Http(code)
        }
    )
} catch (e: SocketTimeoutException) {
    ApiResult.Failure(AppError.Timeout)
} catch (e: IOException) {
    ApiResult.Failure(AppError.Network)
} catch (e: Exception) {
    ApiResult.Failure(AppError.Unknown)
}
