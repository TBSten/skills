---
name: kotlin-maven-central-publish
description: >
  Kotlin/KMP プロジェクトに Maven Central 公開設定を追加する。
  Vanniktech Maven Publish プラグインによる convention plugin (buildSrc / build-logic)、
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
- Gradle + version catalog (`gradle/libs.versions.toml`。`[plugins]` に Kotlin プラグインのエイリアスがあること)
- GitHub でホスティングされていること（GitHub Actions を使用するため）
- bash / git が利用可能なこと (script 実行に必要)

## Step 1: プロジェクト情報の収集

以下をユーザーに確認する。既に明確な場合はスキップしてよい。

1. **Maven Group ID** — 例: `com.example.mylib`
2. **バージョンの置き場所と初期バージョン** — catalog `[versions]` のキー (例: `mylib = "0.1.0"`) か `gradle.properties` の `VERSION_NAME`
3. **ライセンス** — デフォルト: MIT License（script が `LICENSE` ファイルから推定可能）
4. **GitHub リポジトリ URL**（script が `git remote get-url origin` から推定可能）
5. **開発者情報** — ID, 名前, URL（script が GitHub owner / `git config user.name` から推定可能）
6. **公開対象モジュール** — どのサブモジュールを Maven Central に公開するか
7. **convention の置き場所** — 既存の `buildSrc` か、included build (`build-logic` 等) か。既存構成に合わせる

3〜5 は script が自動推定するため、ユーザーが明示した場合だけオプションで渡せばよい。

## Step 2: setup-publish.sh の実行

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/setup-publish.sh" \
  --description "<プロジェクトの説明 (英語)>" \
  --group-id <GROUP_ID> \
  --version-ref <catalog のキー> --version <初期バージョン>   # gradle.properties の VERSION_NAME を使うなら省略
  # build-logic 等に置く場合: --convention-dir build-logic
```

- 必須は `--description` とバージョンの SSoT (`--version-ref` か `gradle.properties` の `VERSION_NAME`)。その他は自動推定され、推定できない場合は「何が・なぜ・どう直すか」がエラー表示されるので従って再実行する。オプション一覧は `--help`
- script がやること (詳細は script が SSoT):
  1. catalog に vanniktech プラグイン (Gradle / Kotlin の版から互換バージョンを自動選択) と自ライブラリのバージョンを冪等追記
  2. convention ビルド (`<convention-dir>/build.gradle.kts` / `settings.gradle.kts`) の生成。vanniktech と同じ classpath に KGP / AGP も載せる
  3. `<convention-dir>/src/main/kotlin/publish-convention.gradle.kts` をプレースホルダー置換して生成
  4. `.github/workflows/publish.yml` (Release 駆動の publish) と `publish-check.yml` (PR で `publishToMavenLocal`) の生成。runner は Apple ターゲットの有無で自動選択
- 冪等。既存ファイルは `--force` なしでは上書きしない
- stdout 末尾 1 行の JSON に結果が出る。`action_required` の各項目は **AI が対応する**:
  - 既存ファイルへのマージ → `example/` の該当ファイルを参照して手動マージ
  - 「バージョン付きのプラグイン要求を置き換える」(buildSrc の時のみ) → 指摘行の `alias(libs.plugins.X)` を `id(libs.plugins.X.get().pluginId)` に置き換える (ルートの `apply false` 行は削除でもよい)
  - `includeBuild` の追加 → ルート `settings.gradle.kts` の `pluginManagement { }` に追加

## Step 3: 各モジュールに convention plugin を適用

**AI の責務**: 公開対象モジュールを判断し、各モジュールの `build.gradle.kts` に以下を追加する:

```kotlin
plugins {
    id("publish-convention")
}

group = "<GROUP_ID>"
version = libs.versions.<versionRef>.get()   // gradle.properties の GROUP / VERSION_NAME を使う場合は 2 行とも不要
```

- モジュール固有の説明は `mavenPublishing { pom { description.set("...") } }` で上書きする。**`coordinates(...)` は呼ばない** (artifactId は convention が `project.path` から導出する)
- Dokka による javadoc・古い Kotlin 向けの `languageVersion` 床・署名方式の変更などの任意設定は `references/convention-options.md`

## Step 4: ローカル動作確認

```bash
./gradlew publishToMavenLocal
```

署名はスキップされるので GPG 鍵が無くても通る。`~/.m2/repository/<group-path>/` に各モジュールの成果物 (pom / jar / sources / javadoc) が生成されていることを確認。
失敗した場合は `references/troubleshooting.md` を参照。

## Step 5: GPG 鍵と GitHub Secrets のセットアップ (setup-secrets.sh)

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/setup-secrets.sh"
```

対話 script。GPG 鍵の生成 (既存鍵は `--key-id`) → `SIGNING_KEY_ID` 抽出 → 公開鍵のキーサーバー送信 → 秘密鍵の ASCII armor export → `gh secret set` による 5 つの Secrets 登録までを自動化する。

ユーザーの手作業は **Sonatype Central Portal の User Token 発行だけ**（script が手順を案内し、入力を促す）。
キーサーバー送信と Secrets 登録の前には確認プロンプトが出る。`--dry-run` で副作用なしの実行計画確認ができる。
gpg / gh が無い等で script が使えない環境では、フォールバック手動手順として `references/gpg-setup.md` / `references/github-secrets.md` を参照する。

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |

## Step 6: 公開

1. バージョンの SSoT をリリース版に上げて main にマージ
2. tag `v<版>` で GitHub Release を publish (pre-release も可) → `publish.yml` が tag とバージョンの一致・非 SNAPSHOT を検証して `publishAndReleaseToMavenCentral`
3. 初回は Actions タブから `workflow_dispatch` (既定 `dry_run: true`) で check と `publishToMavenLocal` だけ流して確認してもよい

workflow の設計理由、tag push 駆動 + Release 自動生成などの代替フローは `references/release-flow.md`。
