---
name: navigation3-main-tab
description: >
  Navigation 3 の SceneStrategy を活用した下タブ (BottomNavigation) 管理パターンを実装するスキル。
  MainTab enum、MainTabSceneStrategy、MainTabScene を組み合わせ、
  タブ切り替え時に Scene を破棄せず中身だけ入れ替える設計を提供する。
  Kotlin Multiplatform + Compose + Navigation 3 プロジェクト向け。
  Use when requested: "Navigation 3 でタブを実装して", "下タブを追加して",
  "BottomNavigation を実装して", "SceneStrategy でタブを管理したい",
  "タブ切り替え時に状態を保持したい", "Navigation 3 の MainTab パターン",
  "implement bottom tabs with Navigation 3".
---

# navigation3-main-tab

Navigation 3 の SceneStrategy を活用して、下タブ (BottomNavigation) を管理するパターンを実装する。

## 前提条件

- Kotlin Multiplatform (or Android) + Compose プロジェクト
- Navigation 3 (`androidx.navigation3`) が依存に含まれていること
- `lifecycle-viewmodel-navigation3` が依存に含まれていること

## 設計概要

Navigation 3 の `SceneStrategy` を使い、タブ画面を `MainTabScene` でラップする。

**核心**: `MainTabScene` の `key` を固定値にすることで、タブ切り替え時に Scene が破棄・再生成されず、
NavigationBar を維持したまま中身だけ切り替わる。

タブ判定は `NavEntry.metadata` に埋め込んだ `MainTab` 値で行い、
metadata がないエントリは通常の `SinglePaneSceneStrategy` にフォールバックする。

## 実装手順

### Step 1: MainTab enum の定義

タブの種類を enum で定義する。`example/` の `MainTab.kt` を参照。

### Step 2: MainTabNavigator interface の定義

タブ切り替えの contract を定義する。`example/` の `MainTabNavigator.kt` を参照。
各 feature モジュールはこの interface に依存し、具体実装は知らない。

### Step 3: MainTabScene の実装

Navigation 3 の `Scene<T>` を実装する。`example/` の `MainTabScene.kt` を参照。

**重要**: `key` を `companion object` 等の固定値にすること。
これによりタブ切り替え時に Scene インスタンスが再利用され、Scaffold + NavigationBar が維持される。

### Step 4: MainTabSceneStrategy の実装

`SceneStrategy<T>` を実装する。`example/` の `MainTabSceneStrategy.kt` を参照。

- `NavEntry.metadata` から `MainTab` を取り出す
- タブ画面なら `MainTabScene` を返し、そうでなければ `null` を返してチェーン先にフォールバック
- `mainTab(tab)` ヘルパーで metadata の Map を生成

### Step 5: MainTabScaffold (UI) の実装

Material3 `NavigationBar` + `NavigationBarItem` でタブバーを構築する。
`example/` の `MainTabScaffold.kt` を参照。タブのアイコン・ラベルはプロジェクトに合わせて変更する。

### Step 6: AppNavigation での統合

`NavDisplay` の `sceneStrategy` に `MainTabSceneStrategy` をチェーンの先頭に配置する:

```kotlin
sceneStrategy = remember {
    MainTabSceneStrategy<Screen>()
        .then(DialogSceneStrategy())
        .then(SinglePaneSceneStrategy())
}
```

`entryProvider` でタブ画面のエントリに metadata を設定する:

```kotlin
entry<Home>(
    metadata = MainTabSceneStrategy.mainTab(MainTab.Home),
) {
    HomeScreen()
}
```

### Step 7: switchTab ロジック

`AppNavigator` の `switchTab` でバックスタック内の最後のタブ画面を in-place 置換する。
`references/switch-tab-logic.md` を参照。

### Step 8: Screen ↔ MainTab マッピング

Screen sealed interface に `mainTabOrNull` 拡張プロパティ、
MainTab に `screen` 拡張プロパティを定義して双方向マッピングを集約する。
`references/screen-tab-mapping.md` を参照。

## ファイル構成 (生成物)

```
ui/feature/mainTab/
├── MainTab.kt                # タブ enum
├── MainTabNavigator.kt       # Navigator interface
├── MainTabScreen.kt          # Composable (ViewModel + Scaffold)
├── MainTabViewModel.kt       # ViewModel
├── MainTabScaffold.kt        # Scaffold + NavigationBar UI
├── MainTabScene.kt           # Scene<T> 実装
└── MainTabSceneStrategy.kt   # SceneStrategy<T> 実装

ui/navigation/
├── Screen.kt                 # Screen sealed interface + MainTab マッピング
├── AppNavigation.kt          # NavDisplay 統合
└── AppNavigator.kt           # バックスタック管理 + switchTab
```

## カスタマイズポイント

- タブの追加: `MainTab` enum にエントリ追加 → `MainTabScaffold` にアイコン/ラベル追加 → `Screen` マッピング追加
- アニメーション: `MainTabScene.content` 内で `AnimatedContent` をラップ
- バッジ: `NavigationBarItem` の `icon` に `BadgedBox` を使用
