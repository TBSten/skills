---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

NewsReader の shared モジュールで Navigation 3 の NavDisplay を使っています (`navigation/AppNavHost.kt`)。ここに下タブ (BottomNavigation) を追加したいです。

- タブにするのは Home / Search / Notifications の 3 つ
- Settings と ArticleDetail はタブ無しの全画面
- タブを切り替えたときに NavigationBar が作り直されてチラつかないように、SceneStrategy でちゃんと管理したい
- 既存の `Navigator` は DeepLink や通知からも使っているので、別の Navigator を新しく作って置き換えるのではなく、今のクラスに統合してください
- 同じタブを何度押してもバックスタックが積み上がらないようにしたい

ビルドは実行しなくて大丈夫です。

Input files: settings.gradle.kts, gradle/libs.versions.toml, shared/build.gradle.kts, shared/src/commonMain/kotlin/jp/news/app/navigation/Route.kt, shared/src/commonMain/kotlin/jp/news/app/navigation/Navigator.kt, shared/src/commonMain/kotlin/jp/news/app/navigation/AppNavHost.kt, shared/src/commonMain/kotlin/jp/news/app/feature/Screens.kt
