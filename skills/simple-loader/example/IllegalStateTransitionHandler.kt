package me.tbsten.simpleloader

object IllegalStateTransitionHandler {
    fun <Reason> `throw`(reason: Reason): Nothing = throw IllegalStateTransitionException(reason)

    fun <Reason> printWarning(reason: Reason) {
        println("WARN: $reason")
    }

    fun <Reason> noOp(reason: Reason) {
        // no-op
    }

    fun <Reason> default(): (Reason) -> Unit = ::`throw`
}

class IllegalStateTransitionException(reason: Any?) : Exception("Illegal state transition: $reason")
