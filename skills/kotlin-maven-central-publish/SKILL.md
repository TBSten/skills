---
name: kotlin-maven-central-publish
description: >
  Kotlin/KMP プロジェクトに Maven Central 公開設定を追加する。
  Vanniktech Maven Publish プラグインによる buildSrc convention plugin、
  GPG 署名、GitHub Actions CI/CD ワークフロー、Sonatype Central Portal 連携を
  一括でセットアップする。
  Use when requested: "Maven Central に公開したい", "ライブラリを publish したい",
  "Maven Central publishing をセットアップ", "publishToMavenLocal できるように",
  "Gradle で Maven Central 公開の設定", "Kotlin ライブラリを公開する設定を追加".
metadata:
  status: Active
  group: Kotlin ライブラリ/ツール開発
---

# kotlin-maven-central-publish

Kotlin/KMP プロジェクトに Maven Central 公開設定を追加する。

機械的なセットアップは `scripts/` の 2 つの script が担う。
**script は読解・書き換え・再実装せず、そのまま実行すること。**
以下、この SKILL.md があるディレクトリを `${CLAUDE_SKILL_DIR}` とする (Claude Code では環境変数として利用できる)。

## 前提条件

- Kotlin プロジェクト (KMP または JVM)
- Gradle + version catalog (`gradle/libs.versions.toml`)
- GitHub でホスティングされていること（GitHub Actions を使用するため）
- bash / git が利用可能なこと (script 実行に必要)

## Step 1: プロジェクト情報の収集

以下をユーザーに確認する。既に明確な場合はスキップしてよい。

1. **Maven Group ID** — 例: `com.example.mylib`
2. **アーティファクトバージョン** — 例: `0.1.0`
3. **ライセンス** — デフォルト: MIT License（script が `LICENSE` ファイルから推定可能）
4. **GitHub リポジトリ URL**（script が `git remote get-url origin` から推定可能）
5. **開発者情報** — ID, 名前, URL（script が GitHub owner / `git config user.name` から推定可能）
6. **公開対象モジュール** — どのサブモジュールを Maven Central に公開するか

3〜5 は script が自動推定するため、ユーザーが明示した場合だけオプションで渡せばよい。

## Step 2: setup-publish.sh の実行

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/setup-publish.sh" \
  --description "<プロジェクトの説明 (英語)>" \
  --group-id <GROUP_ID>
```

- `--description` のみ必須。`--github-url` / `--license` / `--developer-id` / `--developer-name` / `--start-year` 等は未指定なら自動推定され、推定できない場合は「何が・なぜ・どう直すか」がエラー表示されるので、それに従いオプションを追加して再実行する。オプション一覧は `--help`
- script がやること:
  1. `gradle/libs.versions.toml` に `mavenPublish` プラグインを冪等追記
  2. `buildSrc/build.gradle.kts` / `buildSrc/settings.gradle.kts` の生成（既存ファイルがある場合は上書きせず ACTION_REQUIRED を出す）
  3. `buildSrc/src/main/kotlin/publish-convention.gradle.kts` の生成 — `example/publish-convention.gradle.kts` のプレースホルダー（説明・GitHub URL・開始年・ライセンス・開発者情報・owner/repo）を置換済み。プレースホルダーの完全な一覧と置換ロジックは script が SSoT
  4. `.github/workflows/publish.yml` の生成
- 再実行しても安全（冪等）。既存ファイルは `--force` なしでは上書きしない
- stdout 末尾 1 行の JSON に結果が出る。`action_required` に項目がある場合（例: 既存の `buildSrc/build.gradle.kts` に依存を手動マージ）は、`example/` の該当ファイルを参照して **AI が手動でマージする**

## Step 3: 各モジュールに convention plugin を適用

**AI の責務**: 公開対象モジュールを判断し、各モジュールの `build.gradle.kts` に以下を追加する:

```kotlin
plugins {
    id("publish-convention")
}

group = "<GROUP_ID>"
version = libs.versions.<versionRef>.get()
```

## Step 4: ローカル動作確認

```bash
./gradlew publishToMavenLocal
```

成功したら `~/.m2/repository/<group-path>/` にアーティファクトが生成されていることを確認。

## Step 5: GPG 鍵と GitHub Secrets のセットアップ (setup-secrets.sh)

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/setup-secrets.sh"
```

対話 script。以下を自動化する:

- GPG 鍵の生成（既存鍵を使う場合は `--key-id`）
- フィンガープリント末尾 8 桁（`SIGNING_KEY_ID`）の抽出
- 公開鍵のキーサーバー送信
- 秘密鍵の ASCII armor export
- `gh secret set` による 5 つの GitHub Secrets 登録

ユーザーの手作業は **Sonatype Central Portal の User Token 発行だけ**（script が手順を案内し、入力を促す）。
キーサーバー送信と Secrets 登録の前には確認プロンプトが出る。`--dry-run` で副作用なしの実行計画確認ができる。
gpg / gh が無い等で script が使えない環境では、フォールバック手動手順として `references/gpg-setup.md` / `references/github-secrets.md` を参照する。

### 必要な GitHub Secrets (5つ)

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |

## Step 6: 公開テスト

GitHub Release を作成して `.github/workflows/publish.yml` の publish ワークフローが成功することを確認する。
ワークフローは Release 作成時（released / prereleased）に自動実行され、手動実行（workflow_dispatch）にも対応、`publishAndReleaseToMavenCentral` タスクを実行する。
