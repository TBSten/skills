---
name: kotlin-library-bootup
description: >
  普通の Kotlin / Kotlin Multiplatform ライブラリのプロジェクトを、実運用している OSS 群と同じ構成で
  一式立ち上げる。build-logic の convention plugin、version catalog、explicitApi と opt-in マーカー、
  kotest、binary-compatibility-validator、ktlint、vanniktech による Maven Central publish、
  独立ビルドの integrationTest、Dokka + Astro Starlight の docs サイトと GitHub Pages デプロイ、
  GitHub Actions (CI / publish / docs)、README / CLAUDE.md / .claude/rules / 同梱 skill までを生成し、
  ビルド・テスト・publishToMavenLocal まで確認する。ターゲットは standard / full / jvm から選ぶ。
  KSP processor や compiler plugin は対応する skill に委譲する。
  Use when requested: "Kotlin ライブラリを新規作成", "KMP ライブラリをセットアップ", "kotlin library bootup",
  "ライブラリの雛形", "Kotlin Multiplatform ライブラリを作りたい", "Maven Central に出す Kotlin ライブラリを始めたい",
  "新しい OSS ライブラリのプロジェクトを作って".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

# Kotlin Library Bootup

Kotlin / KMP ライブラリのプロジェクトを一式生成し、ビルドが通るところまで確認する。

コピー・条件分岐・プレースホルダー置換・rename・置換漏れ検証は `scripts/scaffold.sh`、
ビルド確認は `scripts/verify.sh` が行う。**AI の責務は、入力値のヒアリング・script の実行・生成物のレビュー・
失敗時の原因分析だけ**。

## 委譲ルーティング

このスキルは「普通のライブラリ」の土台だけを作る。次のものは対応する skill に任せる (土台を作った後に併用してよい)。

| やりたいこと | 使う skill |
|---|---|
| KSP processor | `ksp-plugin-setup` |
| compiler plugin / 複数 Kotlin 版対応 | `kotlin-compiler-plugin-setup` / `kotlin-compiler-plugin-dev` (+ kitakkun/kotlin-compiler-plugin-skills) |
| モジュール横断の収集 (cross-module collection) | `collector-compiler-plugin` |
| snapshot テスト / property-based テストの基盤 | `kmp-snapshot-testing-setup` |
| GPG 鍵の作成・GitHub Secrets の登録 | `kotlin-maven-central-publish` の `setup-secrets.sh` |
| IntelliJ plugin | `intellij-plugin-dev` |

## Usage

### 確認事項

ユーザーの指示から明確に読み取れる項目は確認を省略してよい。一度に全部聞かず、上から順に確認する。

1. **名前** (`--name`) — kebab-case。rootProject 名・convention plugin id・artifactId の接頭辞になる
2. **パッケージ** (`--package`) — 例: `io.github.<owner>.<name>`。groupId は既定で同じ (`--group-id` で変更)
3. **ターゲット** (`--targets`) — 次の 3 つから選んでもらう。判断材料は references/targets.md
   - `standard` (既定): android / jvm / js / wasmJs / iOS
   - `full`: standard + macOS / watchOS / tvOS / Linux / Windows / androidNative / wasmWasi
   - `jvm`: JVM のみ (Android / Apple 関連は生成しない)
4. **含めるもの** (既定は全部入り。外すものだけ聞く)
   - integrationTest (Maven 座標経由で利用者と同じ形で試す独立ビルド) — 外すなら `--skip-integration-test`
   - docs サイト (Astro Starlight + GitHub Pages) — 外すなら `--skip-docs-site`
   - 同梱 skill (bump-library-version / release-note / verify-changes) — 外すなら `--skip-ai-skills`
   - GitHub Actions — 外すなら `--skip-ci`
5. **GitHub の owner / repo** — `--dest` が GitHub の clone なら origin から推定される。推定できない時は聞く
6. 任意: 最初のモジュール名 (`--module-name`、既定 `<name>-core`)、POM の説明・開発者名
   (`--description` / `--developer-id` / `--developer-name`)

Kotlin の版は既定 (catalog の値) のままにする。変える時は `--kotlin-version` と、対応する KSP を
`--ksp-version` で**手動で合わせる** (KSP2 は Kotlin と独立した版番号で、script は追従させない)。

### Step 1: scaffold.sh の実行

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/scaffold.sh" --dry-run \
  --dest <生成先> --name <name> --package <package> --targets <standard|full|jvm> [オプション]
```

`--dry-run` で配置予定を確認してから、同じ引数から `--dry-run` を外して実行する。

**script は読解・書き換え・再実装せず、そのまま実行する。** 引数・置換仕様・条件分岐の記法は
`scaffold.sh --help` が SSoT。生成先に既存ファイルがあると止まるので、上書きしてよいかユーザーに確認してから
`--force` を付ける。置換漏れを検出して止まった場合は skill 側の不具合なので、生成先を手で直さずに報告する。

### Step 2: 生成物のレビュー

出力された配置一覧を見て、次を確認する。

- 名前・パッケージ・groupId・owner/repo が意図どおりか (README 冒頭、`gradle/libs.versions.toml`、
  `build-logic/src/main/kotlin/<name>.publish.gradle.kts` の POM)
- ターゲットが選んだとおりか (`build-logic/src/main/kotlin/Targets.kt`。jvm なら存在しない)
- skip したものの痕跡 (README の Modules 表、CLAUDE.md、ci.yml の job) が残っていないか
- サンプル API (`<Pascal>` クラス) とテストは置き換える前提の雛形であること

構成の各所が「なぜそうなっているか」は references/techniques.md にある。レビューで疑問が出たらそこを読む。

### Step 3: verify.sh

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/verify.sh" --project-dir <生成先> --bootstrap-wrapper --fresh
```

**script は読解・書き換え・再実装せず、そのまま実行する。** 実行する step と順序は `verify.sh --help` が SSoT。
各 step のログは `<生成先>/.local/tmp/` に残る。FAILED があればそのログを読んで原因を調べる
(`grep` / `tail` で雑に切らない)。環境不足 (Android SDK・Gradle・pnpm が無い等) は script が直し方付きで
止まるか SKIPPED を出すので、その内容をユーザーに伝える。

### Step 4: 次の一手

ユーザーに次を案内する。

1. `git init` (まだなら) し、生成物と `*/api/` (BCV の dump) をコミットする
2. サンプル API を実際の API に置き換え、`./gradlew apiDump` し直す
3. Maven Central に出すなら、`kotlin-maven-central-publish` の `setup-secrets.sh` で GPG 鍵と Secrets を登録する
4. docs サイトを使うなら、GitHub の Settings > Pages > Source を "GitHub Actions" にする
5. KSP / compiler plugin / snapshot テスト等が必要なら、上の委譲ルーティングの skill を使う

## リソース

| リソース | いつ読むか |
|---|---|
| scripts/scaffold.sh | Step 1 で実行する (読解・改変はしない)。引数と置換仕様の SSoT |
| scripts/verify.sh | Step 3 で実行する (読解・改変はしない)。step と順序の SSoT |
| references/techniques.md | 生成物の各手法の理由と出典。レビュー時、構成を変えたくなった時 |
| references/targets.md | ターゲット選択の判断材料、Apple ターゲットの有効化条件 |
| references/compatibility-floor.md | Kotlin 下限 (下位互換の床) の仕組みと、KMP の JS/Wasm での制約 |
| references/extensions.md | まだ入れていないもの (Compose / kover / Konsist / context7.json) と script の既知の制限 |
| example/ , assets/project/ | scaffold.sh のコピー元 (直接編集・手動コピーはしない) |
