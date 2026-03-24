package me.tbsten.simpleloader.simple

fun <Data : Any> SimpleLoader.State<Data>.dataOrNull(): Data? = when (this) {
    is SimpleLoader.State.InitialPhase -> null
    is SimpleLoader.State.AfterLoadedPhase<*> -> @Suppress("UNCHECKED_CAST") (this.data as? Data?)
}

inline fun <Data, R : Data> SimpleLoader.State<Data>.dataOr(defaultValue: () -> R): Data? =
    when (this) {
        is SimpleLoader.State.InitialPhase -> defaultValue()
        is SimpleLoader.State.AfterLoadedPhase<*> ->
            runCatching { @Suppress("UNCHECKED_CAST") (data as Data) }
                .fold(
                    onSuccess = { it },
                    onFailure = { defaultValue() },
                )
    }


fun <Data : Any> SimpleLoader.State<Data>.exceptionOrNull(): Throwable? = when (this) {
    is SimpleLoader.State.Error -> this.exception
    else -> null
}
