package com.example.androiderrorhandling.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ErrorStateHolder {
    val errorState: StateFlow<ErrorState>
    fun sendErrorState(errorState: ErrorState)
    fun onClose()
    fun onRetry()
}

abstract class AbstractErrorStateHolder : ErrorStateHolder {
    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.NoError)
    override val errorState = _errorState.asStateFlow()
    
    override fun sendErrorState(errorState: ErrorState) {
        // Optional: logError(errorState)
        _errorState.tryEmit(errorState)
    }

    override fun onClose() {
        _errorState.update {
            if (it !is ErrorState.HandleError) return@update it
            ErrorState.Hide(it)
        }
    }

    override fun onRetry() {
        _errorState.update {
            if (it !is ErrorState.HandleError) return@update it
            it.handleType.retry?.invoke()
            ErrorState.Hide(it)
        }
    }
}
