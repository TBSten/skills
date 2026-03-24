# SimpleLoader Skill

[日本語](./simple-loader.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that generates a SimpleLoader state machine for async data loading in Kotlin/Compose Multiplatform projects.

## Quick Start

### 1. Install the skill:

```bash
npx skills add tbsten/skills --skill simple-loader
```

### 2. Ask your AI agent:

```
Add SimpleLoader to this project for async data loading state management.
```

The skill will detect your project structure, confirm settings, and generate all SimpleLoader files.

## Features

SimpleLoader provides a sealed interface-based state machine for managing async data loading with clear separation between initial load and refresh.

### Generated Files

| File | Description | Required |
|---|---|---|
| `SimpleLoader.kt` | Core interface, State sealed interface, Impl, FakeSimpleLoader | Required |
| `SimpleLoaderFactory.kt` | Factory interface for creating instances | Required |
| `SimpleLoaderExt.kt` | Extension functions (`dataOrNull`, `dataOr`, `exceptionOrNull`) | Optional |
| `SimpleLoaderLogger.kt` | Pluggable logging via `fun interface` | Optional |
| `IllegalStateTransitionHandler.kt` | Policy for illegal transitions (throw/warning/noOp) | Optional |
| `SimpleLoaderWithPartialData.kt` | Partial data support during loading (e.g., progress) | Optional |
| UI helpers (`View.kt`, `AnimatedView.kt`, etc.) | Compose UI state rendering with animations | Optional |

### State Transitions

```
Initial ──initialLoad()──► InitialLoading ──success──► Loaded
                                │                        │
                                └──failure──► InitialError │
                                                          │
                           Loaded ──refresh()──► RefreshLoading ──success──► Loaded
                                                       │
                                                       └──failure──► RefreshError
```

## Usage

### ViewModel

```kotlin
typealias ItemListLoader = SimpleLoader<List<Item>>

class HomeViewModel(
    private val itemListLoader: ItemListLoader,
) : ViewModel(itemListLoader) {
    val loadItemListState by itemListLoader::state

    init { itemListLoader.initialLoad() }
    fun refresh() = itemListLoader.refresh()
}
```

### Compose UI

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.loadItemListState.collectAsStateWithLifecycle()

    state.AnimatedView(
        onInitialErrorRefresh = viewModel::refresh,
        initialLoading = { SkeletonView() },
    ) { state ->
        ItemListView(
            itemList = state.data,
            isRefreshLoading = isRefreshLoading,
        )
    }
}
```

### Testing

```kotlin
val fakeLoader = FakeSimpleLoader<List<String>>(
    coroutineScope = TestScope(),
    state = SimpleLoader.State.Loaded(listOf("item1", "item2")),
)
```

## Repository

This skill is part of [TBSten/skills](https://github.com/TBSten/skills).

```
skills/simple-loader/
├── SKILL.md              ← Installed by `npx skills add`
├── example/              ← Template source files
└── references/           ← Usage pattern documentation
```
