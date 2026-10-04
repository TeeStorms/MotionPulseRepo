package com.example.motionpulse.domain.models

enum class ErrorKind {
    PERMISSION,
    OFFLINE,
    UNKNOWN
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    object Empty : UiState<Nothing>()
    data class Error(val kind: ErrorKind, val message: String, val cause: Throwable? = null) : UiState<Nothing>()
}
