package com.example.androiderrorhandling.error

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface LaunchSafe {
    fun CoroutineScope.launchSafe(
        handleType: ErrorHandleType = ErrorHandleType.Dialog,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job
}

fun LaunchSafe(errorStateHolder: ErrorStateHolder): LaunchSafe = object : LaunchSafe {
    override fun CoroutineScope.launchSafe(
        handleType: ErrorHandleType,
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit,
    ) = launch(
        context = context + CoroutineExceptionHandler { _, throwable ->
            errorStateHolder.sendErrorState(ErrorState.HandleError(throwable, handleType))
        },
        start = start,
        block = block,
    )
}
