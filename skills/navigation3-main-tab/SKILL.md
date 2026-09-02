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
metadata:
  status: Experimental
  group: Kotlin / Android アプリ開発
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

### Step 1: プロジェクト解析と確認

1. `build.gradle.kts` で前提条件の依存 (Navigation 3, `lifecycle-viewmodel-navigation3`) を確認し、不足があれば追加を提案
2. 配置先パッケージとソースディレクトリを既存構成から推定し提案
3. タブ構成 (タブ名・アイコン・対応する Screen) をユーザーの指示や既存コードから把握

### Step 2: install script の実行

`${CLAUDE_SKILL_DIR}/scripts/install.sh` を実行する。
script を読解・書き換え・再実装せず、そのまま実行する。

```bash
"${CLAUDE_SKILL_DIR}/scripts/install.sh" \
  --package <USER_PACKAGE> \
  --dest <TARGET_DIR>
```

- `--dest` は `--package` に対応するソースディレクトリ (例: `app/src/commonMain/kotlin/com/myapp/nav`)。
  `<TARGET_DIR>/maintab/` と `<TARGET_DIR>/navigation/` にファイルが配置される
- 既存ファイルがあるとエラーで停止する。ユーザーが上書きを明示した場合のみ `--force` を付けて再実行
- `--dry-run` で書き込みなしに生成予定を確認できる
- 成功時は stdout 最終行に 1 行 JSON (`{"ok":true,...}`) が出力される。失敗時は stderr の `FIX:` に従う

### Step 3: CUSTOMIZE 箇所の適合

生成ファイルの「必ずプロジェクトに合わせて書き換える箇所」には `// CUSTOMIZE:` コメントが付いている。

```bash
grep -rn "CUSTOMIZE:" <TARGET_DIR>
```

で全箇所を列挙し、Step 1 で把握したタブ構成に合わせて編集する:

- `MainTab.kt` — タブ enum のエントリ
- `MainTabScaffold.kt` — タブのアイコン・ラベル
- `MainTabScreen.kt` — 必要なら ViewModel の DI 注入
- `Screen.kt` — Screen 定義と Screen ↔ MainTab 双方向マッピング (`references/screen-tab-mapping.md` を参照)
- `AppNavigation.kt` — entryProvider のエントリと各画面 Composable の呼び出し
- `AppNavigator.kt` — 初期画面

### Step 4: 既存 NavDisplay への統合

プロジェクトに既に NavDisplay / Navigator の実装がある場合は、生成された `AppNavigation.kt` / `AppNavigator.kt` を丸ごと使わず、既存実装に統合する (プロジェクト構成に応じて判断が必要)。

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

`AppNavigator` の `switchTab` はバックスタック内の最後のタブ画面を in-place 置換する。
生成された `AppNavigator.kt` に実装済み。ロジックの解説は `references/switch-tab-logic.md` を参照。

統合後、プロジェクトのビルドタスク (例: `./gradlew :<module>:compileKotlinJvm` / `:<module>:compileDebugKotlin`) で確認する。

## ファイル構成 (生成物)

```
<TARGET_DIR>/
├── maintab/
│   ├── MainTab.kt                # タブ enum
│   ├── MainTabNavigator.kt       # Navigator interface
│   ├── MainTabScreen.kt          # Composable (Scaffold ラップ)
│   ├── MainTabScaffold.kt        # Scaffold + NavigationBar UI
│   ├── MainTabScene.kt           # Scene<T> 実装
│   └── MainTabSceneStrategy.kt   # SceneStrategy<T> 実装
└── navigation/
    ├── Screen.kt                 # Screen sealed interface + MainTab マッピング
    ├── AppNavigation.kt          # NavDisplay 統合
    └── AppNavigator.kt           # バックスタック管理 + switchTab
```

## カスタマイズポイント

- タブの追加: `MainTab` enum にエントリ追加 → `MainTabScaffold` にアイコン/ラベル追加 → `Screen` マッピング追加
- アニメーション: `MainTabScene.content` 内で `AnimatedContent` をラップ
- バッジ: `NavigationBarItem` の `icon` に `BadgedBox` を使用
