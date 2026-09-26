# 運用: 公開 artifact の検証・バージョン方針・プロジェクト内 skill / rule

compiler plugin を継続運用するときの検証と運用の型。実例は TBSten/debuggable-compiler-plugin (`scripts/`, `.claude/skills/`, `docs/test-strategy.md`) と TBSten/capture-compiler-plugin (`docs/versioning.md`, `.claude/rules/`)。プロジェクトの初期構成 (publish convention / CI / release workflow / TestKit E2E) は `kotlin-compiler-plugin-setup` の scaffold が生成する。

## 1. mavenLocal 経由の smoke test (公開 artifact の検証)

kctfork の unit test は in-process の compiler に Registrar を直接渡すため、次の回帰は検出できない。

- shadow jar に compat module / `META-INF/services` が入っていない
- plugin marker / POM / Gradle Module Metadata の誤り (利用側で解決できない、stdlib 版を引き上げる)
- 実 KGP の `SubpluginArtifact` 解決 (native / wasm 用 classpath を含む)

そこで `publishToMavenLocal` → **独立した consumer プロジェクト** (root build に include しない。`mavenLocal()` から plugin id + version で解決) を対象 Kotlin 版でビルド → `javap -p -c` で生成 bytecode に注入シンボルがあることを確認する。

**実ファイル: [`../assets/scripts/smoke-test.sh`](../assets/scripts/smoke-test.sh)**。`<project-root>/scripts/` にコピーして `chmod +x`。**script は読解・書き換え・再実装せず、そのまま実行する。**

```bash
# 注入されること
scripts/smoke-test.sh --kotlin 2.3.20 --consumer integration-test/smoke \
  --class build/classes/kotlin/jvm/main/example/CounterStore.class --expect debuggableFlow,logAction
# DSL で enabled=false にした consumer では 1 つも注入されないこと (zero-overhead)
scripts/smoke-test.sh --kotlin 2.3.20 --consumer integration-test/smoke-disabled \
  --class build/classes/kotlin/jvm/main/example/CounterStore.class --expect debuggableFlow,logAction --absent
```

consumer の `settings.gradle.kts` は `settings.providers.gradleProperty("integration.kotlin")` を Kotlin plugin 版に使い、Kotlin 版ごとに合う Compose 版などは `when` で選ぶ。KMP consumer では native / wasm / js の `compileKotlin<Target>` も回すと shadow jar の native ロード回帰を拾える。CI では SSOT (`supported-kotlin-versions.txt`) の matrix で回す (`ci-matrix.md`)。

## 2. CI matrix のローカル並列再現

`compiler-plugin-test.sh --all` は直列。全版を手元で速く回したい場合の型 (debuggable `scripts/test-all.sh` / `smoke-test-all.sh`):

- worker ごとに repo を `rsync` した作業コピー (`.local/tmp/test-worker-<version>/`) で Gradle を動かし、`build/` の衝突を避ける。`GRADLE_USER_HOME` は共有 (依存キャッシュは file lock で安全、再ダウンロードを避ける)
- smoke の場合は `publishToMavenLocal` を **最初に 1 回だけ** 実行し、worker は `--skip-publish` (N 並列で `~/.m2` に書き込むと競合する)
- 並列度は `CPU/2` (上限 4) を既定にし、`--serial` / `--parallel N` で変えられるようにする
- 最後に「版 / 結果 / 所要時間 / ログパス」の summary 表を出し、1 つでも失敗なら非 0 終了

## 3. バージョン方針 (semver)

独立リリースの compiler plugin では、公開 API に加えて「対応 Kotlin 版」が互換性の一部になる (capture `docs/versioning.md`):

| 変更 | bump |
|---|---|
| 公開 API (runtime / Gradle DSL / plugin id) の破壊的変更 | MAJOR |
| **対応 Kotlin 版の追加** (compat module 追加等) | MINOR (既存利用者に影響しない) |
| **対応 Kotlin 版の削除** | MAJOR (利用者に Kotlin 更新を強いる) |
| DSL オプション・機能の追加 | MINOR |
| バグ修正・内部リファクタ | PATCH |

compiler plugin の内部 (FIR / IR extension, compat SPI, 診断 ID) は公開 API に含めない。`0.x` の間は MINOR で破壊的変更を許すが、Release notes に明記する。版の SSoT は `gradle.properties` の `VERSION_NAME` 1 か所 (setup scaffold の構成)。

## 4. プロジェクト内 skill: 散らばる更新箇所を一括で扱う

「対応 Kotlin 版」と「ライブラリ版」は 10 ファイル以上に散らばり、手作業だと取りこぼす。プロジェクトの `.claude/skills/` に専用 skill を置く (debuggable の実例):

| skill | 内容 |
|---|---|
| `support-kotlin-version` | 対象版をどの compat module に載せるか判定 → 必要なら新 module → SSOT / CI / README / kctfork・Compose マップを同期 → 全版テスト → 作業メモ更新。本 skill の Step 6 をプロジェクト固有のファイル一覧に具体化したもの |
| `bump-library-version` | SSoT (`gradle.properties`) と同期先 (integration-test の gradle.properties / catalog、README の導入 snippet) を **whitelist で 1 ファイルずつ現在値を読んで** 置換 → 最後に旧版文字列で `grep` sweep して whitelist 漏れを拾う → `chore(release): bump to X.Y.Z` で commit |

書き方のポイント: 冒頭に「なぜこの skill があるか」と **触るファイルの表 (ファイル / 役割 / いつ触るか)** を置く。触らないもの (Android アプリの versionName、サードパーティの版、`.local/`) も明記する。決定的な置換は script にする。

## 5. `.claude/rules/` の `paths` でディレクトリ責務を強制

compat module 構成は「どこに何を置くか」が崩れやすい。capture は 7 本の rule を `paths` glob 付きで置き、該当ディレクトリのファイルを触る時だけ責務を読み込ませている:

```markdown
---
paths:
    - "compiler-plugin/compat/src/main/kotlin/**/compat/**/*.kt"
    - "compiler-plugin/compat-k*/src/main/kotlin/**/*.kt"
---
このディレクトリには Kotlin Compiler API の版差を吸収する SPI と各版の薄い adapter だけを置く。業務ロジックは main module に置く。…
```

- main / compat SPI / 各 `compat-k*` / feature 直下 (`feature/*/*.kt`) / feature 配下 (`feature/*/*/**`) のように、**直下と再帰を glob で区別** して階層ごとに別 rule を当てる
- `RULES.md` に rule 一覧 (対象 path とスコープ) を表で置く

## 6. テスト戦略ドキュメント

`docs/test-strategy.md` に「何をどの層で検証しているか」のカバー状況マトリクスを置くと、新機能・新 Kotlin 版の追加時にテストの置き場所を迷わない (debuggable の型。以下は例):

| 観点 \ 層 | kctfork unit (版 matrix) | IR dump / box | TestKit E2E | mavenLocal smoke (版 matrix) | runtime unit |
|---|---|---|---|---|---|
| 変換結果 | ✅ | ✅ | | ✅ (javap) | |
| Gradle DSL → CLI オプション | | | ✅ | ✅ | |
| 公開 artifact (shadow / marker / metadata) | | | | ✅ | |
| disabled 時 zero-overhead | ✅ | | | ✅ (`--absent`) | |

空欄を「意図的にカバーしない / TODO」と明示しておく。
