# 実 IDE スモーク (Driver) と UI ツリーの覗き方

対話/タイミングは headless で担保できない。実 IDE を薄く駆動する **Driver スモーク**を定期
チェックポイントに置く (毎イテレーションではない)。機能テスト + `renderComposeScene` snapshot で
担保しきれない「ロードされた・tool window が開いた・クリックが効く」レベルを見る。

> **この Driver 層は推奨レシピであり、多くのプラグインでは未配線** — 同梱の `example/` の
> `build.gradle.kts` にも `integrationTest` source set / `testIdeUi` task は無い (`./gradlew tasks` にも
> 出ない)。推奨レシピの雛形として使い、下記を配線したら **最低 1 本の smoke が非ゼロ件で走る**ことまで
> 確認する。
> SKILL 本体のコマンド一覧に "実コマンド" として載せない (未実装を green と誤認させないため)。

## 2 層構成 (機能テスト 厚く + Driver 薄く)

gradle は機能テストと Driver で **source set と JUnit 版を分ける**。

```kotlin
dependencies {
    intellijPlatform { testFramework(TestFrameworkType.Platform) }  // BasePlatformTestCase は JUnit4
    testImplementation("junit:junit:4.13.2")
}
tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }  // ★ test に useJUnitPlatform() を付けない

sourceSets { create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
} }
val integrationTestImplementation by configurations.getting { extendsFrom(configurations.testImplementation.get()) }
dependencies {
    intellijPlatform { testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation") }
    integrationTestImplementation("org.junit.jupiter:junit-jupiter:5.11.4")  // Starter は JUnit5 専用
    integrationTestImplementation("org.kodein.di:kodein-di-jvm:7.20.2")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
}
val integrationTest by intellijPlatformTesting.testIdeUi.registering {
    task {
        // ★ registering だけでは testIdeUi は integrationTest source set を拾わない。出力と runtime
        //    classpath を明示接続しないと src/integrationTest のテストが検出・実行されない。
        val ss = sourceSets.getByName("integrationTest")
        testClassesDirs = ss.output.classesDirs
        classpath = ss.runtimeClasspath
        useJUnitPlatform()   // Starter は JUnit5
    }
}
```

実行 (配線後): `./gradlew test` (機能・ヘッドレス・主軸) / `xvfb-run ./gradlew integrationTest`
(Driver・Linux CI・headful)。

## Driver スモークの守備範囲

- `ui` DSL: `ideFrame { }` 配下で `x(...)` (単一) / `xx(...)` (複数)、ロケータ `xQuery { byAccessibleName /
  byVisibleText / byType("fqcn") / byClass / byAttribute; and/or/contains }`、操作 `.click()` / `keyboard{}`、
  アサート `.should("…") { isVisible() }`。
- **Compose UI は 1 個の `ComposePanel` = Swing ツリー (Driver の XPath) からは中の Jewel 要素・図ノードが
  個別に見えない**。→ Driver は「ロードされた・tool window が開いた・ComposePanel がある」程度まで。
  要素/行/ノードレベルは **機能テスト + `renderComposeScene` snapshot** で担保する (Compose 用 semantics
  テストは持ち込まない方針)。
- **Remote Robot は不採用 (レガシー)**。

## 実 IDE の UI ツリー (locator 作成) の覗き方

1. **Driver 標準 (HTTP)**: IDE を `-Dexpose.ui.hierarchy.url=true` で起動 → 生きた Swing ツリーが
   `http://localhost:<built-in server port>/api/remote-driver/` (既定 63342/63343) に XML で出る。
   `SearchService` はこれを XPath 検索。テストを一時停止してブラウザで開けば `xQuery`/XPath を書ける。
   **Driver/Starter 実行時のみ立つ (素の `runIde` では出ない)**。
2. **自前デバッグ `AnAction` (runIde 中でも使える)**: `internal="true"` の action で
   `java.awt.Window.getWindows()` を歩き、各 `Component` の `class`/`accessiblename`/`visibletext`/`bounds`
   を XML 風にダンプ→ファイル/ログ出力。属性名を Driver の XPath 属性に合わせておくと、Driver 未起動でも
   locator を書ける。
3. **canvas 専用の model/レイアウト矩形ダンプ `AnAction`**: 上記 (2) は canvas 内ノードを出せない
   (1 コンポーネント)。グラフ IR + 計算済み各ノード hit-box をダンプする内部 action を別途用意すると、
   レイアウト目視と hit-test locator (座標) 作成が楽。機能テストの hit-test アサートと資産共有できる。

## 統合ループと AS

- 統合 (実 tool window・PSI/AA・テーマ追従) は `./gradlew runIde` + auto-reload (dynamic plugin, 2020.2+)。
  Compose の IDE 内プレビューより headless PNG がエージェント用主軸。
- 機能テスト (model/AA/PSI) は **IDEA SDK (IU 261) で回して十分**。AS 固有確認 (build 番号スキュー・AS
  同梱プラグインとの相互作用) は実 IDE スモークだけ: Driver をたまに AS ターゲットへ、または手動 `runIde`
  (AS) で目視、程度で足りる (`gotchas.md`)。
- **独立ビルドの CI ゲート**: この plugin モジュール (`<plugin-module>`) は独立 Gradle build なので root の通常 test/check では
  plugin の compile/test/preview が走らない → root/CI に独立 build を明示呼び出しする quality gate を足す。
