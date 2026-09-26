# 採用した手法と理由

scaffold.sh が生成する構成の「なぜ」。どれも実運用しているリポジトリから持ってきたもので、出典を併記する。
生成物を変えたくなった時に、どの前提を崩すことになるかを判断するために読む。

出典の略称: koma-strict / katachi / compose-preview-lab (cpl) / debuggable-compiler-plugin (debuggable) /
capture-compiler-plugin (capture) / cream

## Gradle 基盤

| 手法 | 理由 | 出典 |
|---|---|---|
| `build-logic/` を `pluginManagement { includeBuild(...) }` で取り込む (buildSrc ではない) | buildSrc はルートの classpath に載るため、ルートで `alias(libs.plugins.x)` とバージョン付きで書くと "already on the classpath with an unknown version" になる。included build ならモジュール側も `alias` のまま書ける | koma-strict |
| build-logic も同じ `gradle/libs.versions.toml` を読む | プラグインのバージョンを二重管理しない | koma-strict, cpl |
| `plugin(libs.plugins.x)` ヘルパで plugin marker 座標 (`<id>:<id>.gradle.plugin:<ver>`) に変換して `implementation` する | catalog の `[plugins]` をそのまま使え、`[libraries]` に KGP などの座標を別途書かずに済む | koma-strict |
| precompiled script plugin の中では `libs.version("...")` / `libs.library("...")` で名前引き | precompiled script には catalog の型安全 accessor が生成されない。キーが無ければ設定時に即失敗させる | cpl (util/Gradle.kt), cream |
| ルートの `build.gradle.kts` で全プラグインを `apply(false)` | 全モジュールが同じ classloader のプラグインを使う | koma-strict, cpl |
| 自ライブラリの版を catalog に置き、`allprojects { version = libs.versions.<lib> }` | 版の SSoT を 1 箇所にする。`logVersion` タスクで CI / スクリプトから読める | cpl, koma-strict |
| catalog キーは `foo` と `foo-bar` を併存させない (ビルド定数は camelCase) | accessor が葉と枝の両方になり `asProvider()` が必要になる | design |
| `foojay-resolver-convention` をルートと build-logic の両方の settings に置く | `jvmToolchain(17)` 用の JDK をローカルに無くても自動取得する。build-logic は別ビルドなので両方に必要 | koma-strict, katachi |
| `gradle-daemon-jvm.properties` (toolchainVersion=21) と wrapper の `distributionSha256Sum` | Gradle デーモンの JDK を固定し、wrapper の配布物を検証する | debuggable, capture, koma-strict |
| `kotlin.incremental.js=false` / `.js.klib=false` | KT-82395 (JS の incremental が壊れる) の回避 | debuggable, capture |
| `TYPESAFE_PROJECT_ACCESSORS` | `projects.xxx` で依存を書ける。ルートと同名のサブプロジェクトは衝突するので、scaffold.sh が `--module-name` = `--name` を拒否する | cpl |

## ライブラリ API

| 手法 | 理由 | 出典 |
|---|---|---|
| `explicitApi()` | 公開範囲をコードレビューで見えるようにする | koma-strict, cpl |
| `@Internal<Lib>Api` (ERROR) / `@Experimental<Lib>Api` (WARNING) の opt-in マーカー。自モジュールは convention で opt-in 済み | 利用者にだけ壁を見せる。BCV の `nonPublicMarkers` にも入れて互換性チェックの対象外にする | cpl |
| Android namespace をパッケージ + モジュール名から導出 | モジュールを増やしても手書きしない | cpl (ModuleNameUtils) |

## 品質ゲート

| 手法 | 理由 | 出典 |
|---|---|---|
| binary-compatibility-validator で `apiDump` / `apiCheck`、KMP は `klib.enabled` | JVM 以外の ABI 破壊も PR で検出する | cpl |
| ktlint の除外は絶対パスで判定 (`it.file.absolutePath.contains("/build/")`) | `exclude("**/build/**")` はソースディレクトリ相対で照合されて効かない | ksp-plugin-setup の検証 |
| ルールは `.editorconfig` が SSoT、テスト関数名の規約は外す | テスト名を日本語の文で書くため | cream, cpl |
| テストは kotest FreeSpec。KMP は KSP + kotest plugin で JS/Wasm/Native も実行 | kotest の spec を非 JVM で動かすには launcher 生成 (KSP) が要る。kotest plugin は KSP の後に適用する | cream, koma-strict |
| Android は `withHostTest {}` で commonTest を JVM host でも回す | AGP 9 の KMP android は既定で host test を持たない | ksp-plugin-setup の検証 |
| `integrationTest/` を独立 Gradle ビルドにし、Maven 座標 + `includeBuild("../")` で依存 | 利用者と同じ経路 (公開座標) で検証できる。opt-in の壁や explicitApi の漏れは本体の中からは見えない | katachi (sample), cpl |

## publish

| 手法 | 理由 | 出典 |
|---|---|---|
| vanniktech maven-publish + `publishToMavenCentral()` (引数なし) | `SonatypeHost` は 0.34 で DSL から削除済み | koma-strict, katachi |
| artifactId を project path から導出 (`:a:b` → `a-b`) | モジュールを増やしても座標を手書きしない。composite build の置換 (group + project.name) とも一致する | koma-strict |
| javadoc jar は `JavadocJar.Dokka("dokkaGeneratePublication*")` | `"dokkaGenerate"` は出力を持たない lifecycle タスクで、渡すと空 jar になる。KMP は HTML、JVM は dokka-javadoc | katachi |
| `*ToMavenLocal` の時だけ署名をスキップ (末尾セグメントで判定) | ローカルに GPG 鍵が無くても publishToMavenLocal できる。`:x:publishToMavenLocal` でも効く | koma-strict |
| publish.yml: release `published` トリガ + tag == catalog + 非 SNAPSHOT チェック + `--no-configuration-cache` + concurrency 非 cancel | 版のずれた公開を防ぐ。途中キャンセルで staging を残さない | katachi, koma-strict |
| publish は CI (`workflow_call`) を通してから | 壊れた状態で公開しない | design |
| Secrets は `ORG_GRADLE_PROJECT_*` で注入 | kotlin-maven-central-publish の setup-secrets.sh と名前を揃えている | koma-strict |

## CI

| 手法 | 理由 | 出典 |
|---|---|---|
| lint / api-check / プラットフォーム matrix / publish-local / integration-test / docs を別 job にし、`final` job で集約 | branch protection の required check を `final` 1 つにできる | debuggable |
| concurrency は PR だけ cancel-in-progress | main の各 commit は個別に検証する | katachi |
| `paths-ignore: **/*.md, .local/**` | ドキュメントだけの変更で CI を回さない | debuggable |
| macOS runner は Apple ターゲットの job だけ | 高価な runner の時間を最小化 | koma-strict, cpl |
| 失敗時に `build/reports` を upload | 手元で再現せずに原因を読める | katachi, debuggable |
| `clear-ci-cache.yml` (手動実行) | キャッシュ起因の不可解な失敗の逃げ道 | cpl |

## docs

| 手法 | 理由 | 出典 |
|---|---|---|
| Dokka 集約の出力を `docs/public/api-docs` に直接書き、`generateApiDocs` ラッパーを CI から叩く | Astro が静的に配信できる。Dokka のタスク名が変わってもラッパーだけ直せばよい | katachi |
| docs.yml は lockfile の有無で `--frozen-lockfile` と setup-node の cache を切り替える | lockfile をまだコミットしていない初期状態でも落ちない | design |

## skill 内の保管形式

| 手法 | 理由 |
|---|---|
| `dot-claude/` `dot-github/` `dot-gitignore` のように `.` を `dot-` にして保管し、scaffold.sh が戻す | `.claude/skills` を置くと、このリポジトリやインストール先の Claude Code に skill として読み込まれる。`.gitignore` も実際に効いてしまう |
| ターゲット・オプション別の差分は `@bootup:if` ディレクティブ | example を standard のままコンパイル可能に保ちつつ、分岐を決定的に処理する (記法は scaffold.sh の冒頭コメント) |
