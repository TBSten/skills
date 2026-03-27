# switchTab Logic

## Core Concept

Tab switching replaces the last tab screen **in-place** rather than pushing a new screen.
This prevents tab screens from stacking in the backstack.

## Implementation

```kotlin
override fun switchTab(tab: MainTab) {
    val screen = tab.screen
    val lastTabIndex = backstack.indexOfLast { it.mainTabOrNull != null }
    if (lastTabIndex >= 0) {
        backstack[lastTabIndex] = screen
    } else {
        backstack.add(screen)
    }
}
```

## Why in-place replacement?

If tabs were pushed (added), pressing back from a tab would navigate to the previous tab
instead of exiting the app. In-place replacement ensures:

1. Only one tab screen exists in the backstack at any time
2. Back navigation from a tab exits the app (or goes to the previous non-tab screen)
3. Non-tab screens pushed on top of a tab are preserved

## Backstack examples

```
Initial:      [Home]
Switch Chat:  [ChatList]        ← Home replaced, not Home → ChatList
Push Detail:  [ChatList, Chat]  ← Chat pushed on top
Switch Home:  [Home, Chat]      ← ChatList replaced at index 0
Back:         [Home]            ← Chat popped
```
