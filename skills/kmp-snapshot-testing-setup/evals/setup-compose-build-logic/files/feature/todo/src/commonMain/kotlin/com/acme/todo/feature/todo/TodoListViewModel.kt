package com.acme.todo.feature.todo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TodoListUiState(val items: List<String> = emptyList(), val filter: String = "")

class TodoListViewModel {
    private val _state = MutableStateFlow(TodoListUiState())
    val state: StateFlow<TodoListUiState> = _state

    fun add(item: String) { _state.value = _state.value.copy(items = _state.value.items + item) }
    fun setFilter(filter: String) { _state.value = _state.value.copy(filter = filter) }
}
