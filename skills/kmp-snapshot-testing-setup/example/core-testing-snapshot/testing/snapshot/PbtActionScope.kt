package com.example.snapshot.testing.snapshot

class PbtActionScope<Subject> {
    val actions = mutableListOf<NamedAction<Subject>>()

    operator fun String.invoke(action: suspend Subject.() -> Unit) {
        actions.add(NamedAction(this, action))
    }

    class NamedAction<Subject>(
        val name: String,
        val action: suspend Subject.() -> Unit,
    )
}
