# navigation3-main-tab

Navigation 3 の SceneStrategy を活用した下タブ (BottomNavigation) 管理パターン。Kotlin Multiplatform + Compose プロジェクト向け。

## 概要

Navigation 3 の `SceneStrategy` API を活用して下タブナビゲーションを実装するスキル。
`MainTabScene` の `key` を固定値にすることで、タブ切り替え時に Scaffold を破棄せず中身だけ入れ替える設計。

## アーキテクチャ

```
MainTabSceneStrategy  ← NavEntry.metadata から MainTab を判定
    ↓
MainTabScene          ← 固定 key → タブ切り替え時に Scaffold を維持
    ↓
MainTabScreen         ← ViewModel + Scaffold + NavigationBar
    ↓
NavEntry.Content()    ← 実際のタブコンテンツ (Home, Chat, MyPage 等)
```

### 主要コンポーネント

| コンポーネント | 役割 |
|---|---|
| `MainTab` | タブ種類を定義する enum |
| `MainTabNavigator` | タブ切り替えの contract (feature モジュールの依存) |
| `MainTabScene` | 固定 `key` を持つ `Scene<T>` 実装 |
| `MainTabSceneStrategy` | タブエントリをラップする `SceneStrategy<T>` |
| `MainTabScaffold` | Material3 `NavigationBar` を含む UI |
| `Screen.mainTabOrNull` | Screen ↔ MainTab の双方向マッピング |

### SceneStrategy チェーン

```kotlin
MainTabSceneStrategy<Screen>()       // タブ画面 → MainTabScene
    .then(DialogSceneStrategy())      // ダイアログ画面
    .then(SinglePaneSceneStrategy())  // その他の画面 (フォールバック)
```

### metadata によるタブ判定

タブ画面は型チェックではなく `NavEntry.metadata` で判定する:

```kotlin
entry<Home>(
    metadata = MainTabSceneStrategy.mainTab(MainTab.Home),
) { HomeScreen() }
```

### switchTab: in-place 置換

タブ切り替えはバックスタック内の最後のタブ画面を置換する (push ではない)。
タブ画面がスタックに積み重ならないようにする。

## なぜ公式レシピではなく SceneStrategy を使うのか？

Navigation 3 の公式レシピでは、トップレベルの `Scaffold` で `NavDisplay` を囲み、
`NavigationBar` をナビゲーショングラフの外側に配置する方法が紹介されている。
セットアップは簡単だが、**全ての画面が Scaffold の内側に描画される**という大きなデメリットがある。

そのため、全画面表示が必要な画面（没入型メディアプレーヤー、オンボーディングフロー、カメラ画面等）の
実装が難しくなる。`NavigationBar` と Scaffold の padding が常に存在するためである。
ルートに応じてバーを条件付きで非表示にする回避策は複雑さを増し、
画面遷移時に視覚的なグリッチを引き起こす可能性がある。

本スキルの `SceneStrategy` アプローチでは、**タブ画面のみ**を Scaffold でラップする。
非タブ画面は `SinglePaneSceneStrategy` にフォールスルーし、タブ UI なしで描画されるため、
全画面表示が自然に実現できる。

| アプローチ | タブ画面 | 非タブ画面 | 全画面表示 |
|---|---|---|---|
| 公式レシピ (トップレベル Scaffold) | Scaffold で囲まれる | Scaffold で囲まれる | 回避策が必要 |
| 本スキル (SceneStrategy) | MainTabScene で Scaffold | Scaffold なし | そのまま可能 |

## 前提条件

- Navigation 3 (`androidx.navigation3:navigation3-ui`)
- Lifecycle ViewModel Navigation 3 (`androidx.lifecycle:lifecycle-viewmodel-navigation3`)
- Compose Material3

## ファイル一覧

| ファイル | 説明 |
|---|---|
| `example/.../maintab/MainTab.kt` | タブ enum |
| `example/.../maintab/MainTabNavigator.kt` | Navigator interface |
| `example/.../maintab/MainTabScene.kt` | Scene 実装 |
| `example/.../maintab/MainTabSceneStrategy.kt` | SceneStrategy 実装 |
| `example/.../maintab/MainTabScaffold.kt` | NavigationBar を含む UI |
| `example/.../maintab/MainTabScreen.kt` | Screen composable |
| `example/.../navigation/Screen.kt` | Screen 定義 + タブマッピング |
| `example/.../navigation/AppNavigation.kt` | NavDisplay 統合 |
| `example/.../navigation/AppNavigator.kt` | バックスタック + switchTab |
| `references/switch-tab-logic.md` | switchTab ロジック解説 |
| `references/screen-tab-mapping.md` | Screen ↔ MainTab マッピングガイド |
