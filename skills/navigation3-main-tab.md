# navigation3-main-tab

Bottom tab (BottomNavigation) management pattern using Navigation 3's SceneStrategy for Kotlin Multiplatform + Compose projects.

## Install

```sh
npx skills add tbsten/skills \
  --skill navigation3-main-tab
```

## Overview

This skill implements a bottom tab navigation pattern that leverages Navigation 3's `SceneStrategy` API. The key insight is using a custom `Scene` with a **fixed key** to preserve the tab scaffold across tab switches, while only swapping the inner content.

## Architecture

```
MainTabSceneStrategy  ← Checks NavEntry.metadata for MainTab
    ↓
MainTabScene          ← Fixed key → preserves Scaffold across tab switches
    ↓
MainTabScreen         ← ViewModel + Scaffold + NavigationBar
    ↓
NavEntry.Content()    ← Actual tab content (Home, Chat, MyPage, etc.)
```

### Key Components

| Component | Role |
|---|---|
| `MainTab` | Enum defining available tabs |
| `MainTabNavigator` | Interface for tab switching (feature module dependency) |
| `MainTabScene` | `Scene<T>` implementation with fixed `key` |
| `MainTabSceneStrategy` | `SceneStrategy<T>` that wraps tab entries |
| `MainTabScaffold` | UI with Material3 `NavigationBar` |
| `Screen.mainTabOrNull` | Bidirectional Screen ↔ MainTab mapping |

### SceneStrategy Chain

```kotlin
MainTabSceneStrategy<Screen>()       // Tab screens → MainTabScene
    .then(DialogSceneStrategy())      // Dialog screens
    .then(SinglePaneSceneStrategy())  // All other screens (fallback)
```

### Tab Detection via Metadata

Tab screens are identified by `NavEntry.metadata`, not by type checking:

```kotlin
entry<Home>(
    metadata = MainTabSceneStrategy.mainTab(MainTab.Home),
) { HomeScreen() }
```

This decouples the tab strategy from specific screen types.

### switchTab: In-Place Replacement

Tab switching replaces the last tab screen in the backstack rather than pushing,
preventing tab screens from stacking.

## Why SceneStrategy instead of the official recipe?

The official Navigation 3 recipe for bottom tabs uses a top-level `Scaffold` that wraps `NavDisplay`,
placing the `NavigationBar` outside of the navigation graph. While simpler to set up, this approach
has a significant drawback: **every screen is rendered inside the Scaffold**, including non-tab screens.

This means full-screen experiences (e.g., immersive media players, onboarding flows, camera screens)
become difficult to implement because the `NavigationBar` and Scaffold padding are always present.
Workarounds like conditionally hiding the bar based on the current route add complexity and
can cause visual glitches during transitions.

The `SceneStrategy` approach in this skill solves this by **only wrapping tab screens** with the Scaffold.
Non-tab screens fall through to `SinglePaneSceneStrategy` and are rendered without any tab chrome,
making full-screen displays trivial.

| Approach | Tab screens | Non-tab screens | Full-screen support |
|---|---|---|---|
| Official recipe (top-level Scaffold) | Scaffold wraps all | Scaffold wraps all | Requires workarounds |
| This skill (SceneStrategy) | Scaffold via MainTabScene | No Scaffold | Works naturally |

## Prerequisites

- Navigation 3 (`androidx.navigation3:navigation3-ui`)
- Lifecycle ViewModel Navigation 3 (`androidx.lifecycle:lifecycle-viewmodel-navigation3`)
- Compose Material3

## Files

| File | Description |
|---|---|
| `example/.../maintab/MainTab.kt` | Tab enum |
| `example/.../maintab/MainTabNavigator.kt` | Navigator interface |
| `example/.../maintab/MainTabScene.kt` | Scene implementation |
| `example/.../maintab/MainTabSceneStrategy.kt` | SceneStrategy implementation |
| `example/.../maintab/MainTabScaffold.kt` | UI with NavigationBar |
| `example/.../maintab/MainTabScreen.kt` | Screen composable |
| `example/.../navigation/Screen.kt` | Screen definitions + tab mapping |
| `example/.../navigation/AppNavigation.kt` | NavDisplay integration |
| `example/.../navigation/AppNavigator.kt` | Backstack + switchTab |
| `references/switch-tab-logic.md` | switchTab logic explanation |
| `references/screen-tab-mapping.md` | Screen ↔ MainTab mapping guide |
