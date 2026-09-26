---
name: verify-changes
description: >-
  example-lib のコード・ビルド設定・ドキュメントを変更したあとの検証手順。変更箇所ごとに
  どの Gradle タスクを回すか、何は回さなくてよいか、ログの残し方と並列実行の作法をまとめている。
  「テストして」「検証して」「CI 通る?」「push 前に確認」と言われたとき、変更を commit する前に使う。
---

# 変更後の検証

## どこまで回すか

| 変更した場所 | 回すもの |
|---|---|
| `example-lib-core` の内部実装 (`private` / `internal`) | `jvmTest` → 最後に `build` |
<!-- @bootup:if integration-test -->
| 公開 API を足した / 変えた | 上 + `apiCheck` (落ちたら `apiDump` して `*.api` の差分を読む) + `-p integrationTest test` |
<!-- @bootup:end -->
<!-- @bootup:if !integration-test -->
| 公開 API を足した / 変えた | 上 + `apiCheck` (落ちたら `apiDump` して `*.api` の差分を読む) |
<!-- @bootup:end -->
<!-- @bootup:if integration-test -->
| `@InternalExampleLibApi` / `@ExperimentalExampleLibApi` の付け外し | 上と同じ。opt-in の壁は `integrationTest` でしか見えない |
<!-- @bootup:end -->
<!-- @bootup:if !integration-test -->
| `@InternalExampleLibApi` / `@ExperimentalExampleLibApi` の付け外し | 上と同じ |
<!-- @bootup:end -->
| `expect` / `actual`、プラットフォーム固有コード | `allTests` (該当ターゲットの `<target>Test` だけでもよい) |
<!-- @bootup:if integration-test -->
| `build-logic/`、`gradle/libs.versions.toml`、`*.gradle.kts` | `help` → `build` → `-p integrationTest test` → `publishToMavenLocal` |
<!-- @bootup:end -->
<!-- @bootup:if !integration-test -->
| `build-logic/`、`gradle/libs.versions.toml`、`*.gradle.kts` | `help` → `build` → `publishToMavenLocal` |
<!-- @bootup:end -->
| KDoc だけ | `generateApiDocs` (Dokka が警告・失敗しないか) |
<!-- @bootup:if docs-site -->
| `docs/` (Astro サイト) だけ | `cd docs && pnpm build`。Gradle は不要 |
<!-- @bootup:end -->
| `README*.md` / `CLAUDE.md` / `.claude/**` だけ | ビルド不要。README は en/ja が揃っているか確認 (`.claude/rules/markdown.md`) |

push 前はどの場合も `ktlintCheck` を通す (整形は `ktlintFormat`)。
IDE からなら run config の `🟣 prepare push` (`apiDump ktlintFormat`) を回す。

## CI と同じもの

`.github/workflows/ci.yml` が `pull_request` で回すものと同等 (ローカルでこれが緑なら CI もほぼ緑):

```bash
./gradlew ktlintCheck apiCheck
./gradlew allTests
./gradlew publishToMavenLocal
# @bootup:if integration-test
./gradlew -p integrationTest test
# @bootup:end
# @bootup:if docs-site
(cd docs && pnpm install --frozen-lockfile && pnpm build)
# @bootup:end
```

Apple ターゲット (iOS 等) のテストは macOS でしか回らない。Linux / Xcode の無い環境では
該当タスクがスキップされるのは正常。

## 何はテストしなくて良いか

<!-- @bootup:if docs-site -->
- **`docs/` のビルド** — Kotlin コードを変えただけなら不要。KDoc の見え方は `generateApiDocs` で確認する
<!-- @bootup:end -->
- **全ターゲットのテスト** — `commonMain` の純粋なロジックだけを変えたなら `jvmTest` で十分。
  `expect` / `actual` やプラットフォーム依存コードを触ったときだけ `allTests`
- **Maven Central への publish** — ローカルでは `publishToMavenLocal` まで。本番 publish は GitHub Release 経由

## 実行の作法

Gradle は `scripts/gradlew-logged.sh` 経由で回す。**スクリプトを読解・書き換え・再実装せず、そのまま実行する。**
出力をすべて `.local/tmp/<時刻>-<name>-<task>.log` に保存し、最後に `LOG <path>` と `EXIT <code>` を出す。

```bash
bash .claude/skills/verify-changes/scripts/gradlew-logged.sh -- jvmTest
bash .claude/skills/verify-changes/scripts/gradlew-logged.sh -- ktlintCheck apiCheck --continue
# @bootup:if integration-test
bash .claude/skills/verify-changes/scripts/gradlew-logged.sh --project integrationTest -- test
# @bootup:end
```

- `EXIT 0` 以外なら `LOG` のファイルを読んで原因を探す。`grep` / `tail` で雑に切って肝心の行を捨てない
- **複数の agent が並列に Gradle を回すときは `--name` を分ける。** `--project-cache-dir` が
  `.local/tmp/gradle-cache/<name>` に分かれ、キャッシュの取り合いを避けられる
  (`build/` 出力は共有なので、同じモジュールを同時に回すのは避ける)
<!-- @bootup:if integration-test -->
- `integrationTest` は `includeBuild("../")` で本体をビルドするので、本体のタスクと並列に回さない
<!-- @bootup:end -->

## 差分の確認

```bash
git status --short
git diff --stat
```

新規ファイルは `git diff` に出ない (untracked)。`api/*.api` を新規作成したときに「差分なし」と
読み違えないよう、`git status --short` と必ず併用する。

`.local/` はコミットしない。
