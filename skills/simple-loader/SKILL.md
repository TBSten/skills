---
name: simple-loader
description: >
  Generates a SimpleLoader state machine for async data loading in Kotlin/Compose Multiplatform projects.
  Provides a sealed interface State with Initial/InitialLoading/InitialError/Loaded/RefreshLoading/RefreshError transitions,
  Factory, Logger, IllegalStateTransitionHandler, FakeSimpleLoader for testing, Compose UI helpers (View, AnimatedView),
  and WithPartialData for progress tracking during loading.
  Use when requested: "SimpleLoader を作って", "データ読み込みの状態管理を実装して",
  "ローディング状態を管理したい", "Loading/Error/Loaded の状態遷移",
  "非同期データ取得のステートマシン", "初回読み込みとリフレッシュを区別したローダー",
  "Compose でローディングのアニメーション遷移", "PartialData でプログレス表示".
  For Kotlin/KMP + Compose Multiplatform projects that need structured async data loading with state management.
metadata:
  status: Active
  group: Kotlin / Android アプリ開発
---

# SimpleLoader Generation Skill

非同期データ読み込みの状態管理を sealed interface ベースのステートマシンとして生成する。
初回読み込み (initialLoad) とリフレッシュ (refresh) を明確に区別し、Compose UI ヘルパーと組み合わせて使う。

## Usage

### 確認事項

コード生成前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **対象モジュール** — SimpleLoader を配置するモジュール (例: `ui/core`, `shared`)
2. **パッケージ名** — 既存の構成から推定し提案
3. **生成ファイル (parts)** — 以下から選択 (デフォルト: 全て)
   - [x] `core` — `SimpleLoader.kt` + `SimpleLoaderFactory.kt` (必須)
   - [x] `logger` — `SimpleLoaderLogger.kt` (core が参照するため必須)
   - [x] `handler` — `IllegalStateTransitionHandler.kt` (core が参照するため必須)
   - [x] `ext` — `SimpleLoaderExt.kt` (`dataOrNull`, `exceptionOrNull`)
   - [x] `partial` — `SimpleLoaderWithPartialData.kt` (部分データ対応)
   - [x] `ui` — UI ヘルパー (`View.kt`, `AnimatedView.kt`, `InitialLoadingView.kt`, `InitialErrorView.kt`)

### スキップ条件

以下の場合は確認をスキップしてデフォルト設定で生成:
- "デフォルトで" / "全部生成して" / "with default settings"

## 生成手順

### Step 1: プロジェクト解析

1. `build.gradle.kts` を確認し KMP / Android / JVM を判定
2. ソースディレクトリのパターンを特定:
   - KMP: `<module>/src/commonMain/kotlin/`
   - Android/JVM: `<module>/src/main/kotlin/`
3. 既存の Loader 系クラスがないか検索

### Step 2: install script の実行

`${CLAUDE_SKILL_DIR}/scripts/install.sh` を実行する。
script を読解・書き換え・再実装せず、そのまま実行する。

```bash
"${CLAUDE_SKILL_DIR}/scripts/install.sh" \
  --package <USER_PACKAGE> \
  --dest <TARGET_DIR> \
  --parts core,logger,handler,ext,partial,ui  # 確認事項 3 の選択。省略時は全て
```

- `--dest` は `--package` に対応するソースディレクトリ (例: `shared/src/commonMain/kotlin/com/myapp/loader`)
- `core` / `logger` / `handler` は相互参照のため常に生成される (省略しても自動追加)
- 既存ファイルがあるとエラーで停止する。ユーザーが上書きを明示した場合のみ `--force` を付けて再実行
- `--dry-run` で書き込みなしに生成予定を確認できる
- 成功時は stdout 最終行に 1 行 JSON (`{"ok":true,...}`) が出力される。失敗時は stderr の `FIX:` に従う

生成されるファイル構成:

```
<TARGET_DIR>/
├── IllegalStateTransitionHandler.kt
└── simple/
    ├── SimpleLoader.kt           # interface + State sealed interface + Impl + Fake
    ├── SimpleLoaderFactory.kt    # Factory interface
    ├── SimpleLoaderExt.kt        # dataOrNull, dataOr, exceptionOrNull
    ├── SimpleLoaderLogger.kt     # fun interface Logger
    ├── SimpleLoaderWithPartialData.kt  # PartialData support
    └── ui/
        ├── View.kt               # State<Data>.View() composable
        ├── AnimatedView.kt       # State<Data>.AnimatedView() with transitions
        ├── InitialLoadingView.kt # Default loading indicator
        └── InitialErrorView.kt   # Default error with retry button
```

### Step 3: 依存関係の確認

`build.gradle.kts` に以下が含まれているか確認し、不足があれば追加を提案:

- `kotlinx-coroutines-core` — 必須
- `kotlinx-serialization` — State の `@Serializable` に必要 (オプション)
- `androidx.compose` — UI ヘルパーに必要
- `androidx.lifecycle` — ViewModel 連携に必要

### Step 4: ビルド確認

```bash
# KMP
./gradlew :<module>:compileKotlinJvm

# Android
./gradlew :<module>:compileDebugKotlin
```

### Step 5: 完了メッセージ

```
## 生成完了

**パッケージ**: `<package>`
**出力先**: `<output_dir>`

### 生成ファイル
- SimpleLoader.kt + SimpleLoaderFactory.kt
- ...

### 依存関係
- [変更なし / 追加: ...]

### ビルド結果
- [SUCCESS / FAILED]
```

## 利用パターン

SimpleLoader の実際の利用方法を以下に示す。references/ 内に詳細なサンプルコードがある。

### パターン 1: typealias + Factory で Loader を定義

```kotlin
// 1. ドメイン固有の Loader 型を typealias で定義
typealias ItemListLoader = SimpleLoader<List<Item>>

// 2. Factory で実装を生成し、DI でバインド
class ItemListLoaderImpl(
    getItemListUseCase: GetItemListUseCase,
    simpleLoaderFactory: SimpleLoaderFactory,
) : ItemListLoader by simpleLoaderFactory.create(
    coroutineScope = MainScope(),
    load = { getItemListUseCase() },
)
```

### パターン 2: ViewModel での利用

```kotlin
class HomeViewModel(
    private val itemListLoader: ItemListLoader,
) : ViewModel(itemListLoader) {  // AutoCloseable として渡す
    // StateFlow をデリゲートで公開
    val loadItemListState by itemListLoader::state

    init {
        itemListLoader.initialLoad()  // 初回読み込み
    }

    fun refresh() = itemListLoader.refresh()  // リフレッシュ
}
```

### パターン 3: Compose UI での状態表示

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.loadItemListState.collectAsStateWithLifecycle()

    state.AnimatedView(
        onInitialErrorRefresh = viewModel::refresh,
        initialLoading = { SkeletonView() },  // カスタムローディング
    ) { state ->
        // ViewWithDataScope 内: isRefreshLoading, isRefreshError が使える
        ItemListView(
            itemList = state.data,
            isRefreshLoading = isRefreshLoading,
        )
    }
}
```

### パターン 4: テストでの利用

```kotlin
// FakeSimpleLoader でテスト
val fakeLoader = FakeSimpleLoader<List<String>>(
    coroutineScope = TestScope(),
    state = SimpleLoader.State.Loaded(listOf("item1", "item2")),
)
val viewModel = HomeViewModel(itemListLoader = fakeLoader)
```

## 状態遷移図

```
Initial ──initialLoad()──► InitialLoading ──success──► Loaded
                                │                        │
                                └──failure──► InitialError  │
                                                         │
                           Loaded ──refresh()──► RefreshLoading ──success──► Loaded
                                                      │
                                                      └──failure──► RefreshError
```

- `load()` は現在の状態に応じて `initialLoad()` か `refresh()` を自動選択する
- Loading 中の `load()` 呼び出しは `IllegalStateTransitionReason` で通知
