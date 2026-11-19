---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

blog-reader の shared モジュールには、もう SimpleLoader 一式 (`jp.blog.app.loader`) が入っています。
これを使って記事一覧画面を作ってください。

- `ArticleRepository.fetchLatest()` で記事一覧を取ってくる Loader
- `ArticleListViewModel`: 画面表示時に初回読み込み、引っ張って更新でリフレッシュ
- `ArticleListScreen` (Compose): 初回ロード中・初回エラー (リトライボタン付き)・表示中をアニメーション付きで切り替え、リフレッシュ中はリストを半透明に
- ViewModel のテストも 1 本書いてほしい (Loaded 状態のときに記事が見えること)

パッケージは `jp.blog.app.feature.articles` でお願いします。ビルドは実行しなくて OK です。

Input files: settings.gradle.kts, shared/build.gradle.kts, shared/src/commonMain/kotlin/jp/blog/app/domain/Article.kt, shared/src/commonMain/kotlin/jp/blog/app/loader/
