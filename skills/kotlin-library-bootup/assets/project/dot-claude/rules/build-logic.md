---
paths:
  - "build-logic/**"
  - "gradle/libs.versions.toml"
  - "**/build.gradle.kts"
  - "**/settings.gradle.kts"
  - "gradle.properties"
---

# ビルドロジック規約

## version catalog が SSoT

- 依存・plugin・自ライブラリの版 (`example-lib`) は `gradle/libs.versions.toml` にだけ書く。
  `build.gradle.kts` や convention plugin にバージョン文字列を直書きしない
- ビルド定数 (jvmToolchain / compileSdk / minSdk / Kotlin の languageVersion 等) も catalog の
  `[versions]` に置く。convention plugin からは `libs.versions.xxx` で読む
- `build-logic/` は `settings.gradle.kts` で同じ catalog を共有している。plugin の依存は
  `plugin(libs.plugins.xxx)` のようなヘルパで marker 座標に変換して足す (二重管理しない)
<!-- @bootup:if ai-skills -->
- 自ライブラリの版は `.claude/skills/bump-library-version/` で上げる。手で書き換えない
<!-- @bootup:end -->
<!-- @bootup:if !ai-skills -->
- 自ライブラリの版を上げたら、README / docs に書いた Maven 座標の版も同じ変更で揃える
<!-- @bootup:end -->

## 「なぜ」をコメントに残す

ビルド設定は後から理由が分からなくなる代表。**値を決めた理由・回避策の出典をコメントで残す。**

```toml
[versions]
# Lowered from the latest so that consumers on Kotlin 2.2 can still use the library.
# coreLibrariesVersion must match this (see build-logic).
kotlin-language = "2.2"
```

- バージョンを下げた・固定したときは、理由と出典 (issue URL 等) を書く
- workaround には、外せる条件を書く (例: `KT-xxxxx が直ったら削除`)
- コメントは英語

## catalog キー衝突の罠

Gradle は catalog のキーの `-` `_` `.` を区切りとしてアクセサを生成する。
**あるキーが別のキーの接頭辞になっていると、短い方のアクセサが「グループ」に化ける。**
`libs.versions.kotlin` が値ではなく `kotlin.xxx` の入れ物になり、`libs.versions.kotlin.get()` と
書いていた箇所がコンパイルエラーになる (`.asProvider()` が必要になる)。既存の参照が
キーを 1 つ足しただけで壊れるので、気付きにくい。

```toml
# NG: libs.versions.kotlin が「値」ではなく kotlin.language を持つグループになる
kotlin = "2.4.10"
kotlin-language = "2.2"

# OK: 接頭辞にならない名前にする
kotlin = "2.4.10"
kotlinLanguage = "2.2"
```

- 新しいキーを足すときは、既存キーの接頭辞 / 既存キーを接頭辞に持つ形になっていないか確認する
- 自ライブラリの版 `example-lib` も同様。`example-lib-xxx` を `[versions]` に足さない
  (`[libraries]` 側は `[versions]` と名前空間が別なので問題ない)

## convention plugin

- モジュール共通の設定は `build-logic/` の convention plugin に寄せる。各モジュールの
  `build.gradle.kts` は plugin の適用とモジュール固有の依存だけにする
- 新しいモジュールは既存の convention plugin (`example-lib.kmp` / `example-lib.jvm` /
  `example-lib.publish` 等) の組み合わせで作る。モジュールごとに設定をコピーしない
- configuration cache / project isolation を壊さない。`allprojects { }` / `subprojects { }` で
  他プロジェクトを設定しない、タスク実行時に `project` を参照しない

## 変更後の確認

```bash
./gradlew help                       # 設定フェーズが通るか (catalog・plugin 解決)
./gradlew build                      # 全体
# @bootup:if integration-test
./gradlew -p integrationTest test    # catalog を共有しているので結合テストも回す
# @bootup:end
```
