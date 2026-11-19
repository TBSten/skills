---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

RecipeBook (Compose Multiplatform) に Navigation 3 で下タブを付けたいです。今はまだナビゲーションが何も無くて、App.kt で FeedScreen を直接出しているだけです。

- タブは「フィード」(Icons.Default.Home)、「さがす」(Icons.Default.Search)、「マイページ」(Icons.Default.Person) の 3 つ
- レシピ詳細 (RecipeDetailScreen, recipeId を受け取る) はタブの上に積む非タブ画面
- タブを切り替えても NavigationBar ごと作り直されないようにしたい
- ナビゲーション関連は `com.hoge.recipe.nav` パッケージにまとめてください

画面の Composable は `ui/Screens.kt` にあります。ビルドは実行しなくて OK です。

Input files: settings.gradle.kts, gradle/libs.versions.toml, composeApp/build.gradle.kts, composeApp/src/commonMain/kotlin/com/hoge/recipe/App.kt, composeApp/src/commonMain/kotlin/com/hoge/recipe/ui/Screens.kt
