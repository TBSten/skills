# Screen ↔ MainTab Mapping

## Purpose

Centralize the bidirectional mapping between `Screen` and `MainTab` in one file
to maintain a single source of truth.

## Pattern

Define two extension properties on `Screen.kt`:

```kotlin
/** Screen → MainTab (nullable, non-tab screens return null) */
val Screen.mainTabOrNull: MainTab?
    get() = when (this) {
        Home -> MainTab.Home
        ChatList -> MainTab.ChatList
        MyPage -> MainTab.MyPage
        else -> null
    }

/** MainTab → Screen */
val MainTab.screen: Screen
    get() = when (this) {
        MainTab.Home -> Home
        MainTab.ChatList -> ChatList
        MainTab.MyPage -> MyPage
    }
```

## Adding a new tab

When adding a new tab, update both properties simultaneously:

1. Add the new entry to `MainTab` enum
2. Add the mapping in `mainTabOrNull`
3. Add the mapping in `MainTab.screen`
4. Add icon/label in `MainTabScaffold`
5. Add `entry<...>(metadata = MainTabSceneStrategy.mainTab(...))` in `AppNavigation`

The Kotlin compiler's exhaustive `when` ensures you don't miss any case.
