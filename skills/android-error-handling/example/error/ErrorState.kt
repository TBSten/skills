package com.example.androiderrorhandling.error

sealed interface ErrorState {
    data object NoError : ErrorState
    data class HandleError(val exception: Throwable, val handleType: ErrorHandleType) : ErrorState
    data class Hide(val handleError: HandleError) : ErrorState
}

sealed interface ErrorHandleType {
    val retry: (() -> Unit)?
    data object Dialog : ErrorHandleType { override val retry = null }
    data object Ignore : ErrorHandleType { override val retry = null }
}
