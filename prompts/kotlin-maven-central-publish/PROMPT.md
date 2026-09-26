# Kotlin プロジェクトの Maven Central 公開セットアップ

このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/kotlin-maven-central-publish` として配布されている一回限りのプロンプト。Kotlin/KMP プロジェクトに Vanniktech Maven Publish プラグインによる convention plugin (buildSrc / build-logic)、GPG 署名、GitHub Actions CI/CD ワークフロー、Sonatype Central Portal 連携を一括でセットアップし、Maven Central への公開設定を追加する。

機械的なセットアップは同リポジトリ配布の 2 つの script が担う。**script は読解・書き換え・再実装せず、curl で取得してそのまま実行すること。**（script は必要なテンプレートを GitHub raw から自動取得する）

## 前提条件

以下を満たしていることを確認する。

- Kotlin プロジェクト (KMP または JVM)
- Gradle + version catalog (`gradle/libs.versions.toml`。`[plugins]` に Kotlin プラグインのエイリアスがあること)
- GitHub でホスティングされていること（GitHub Actions を使用するため）
- bash / git / curl が利用可能なこと

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
curl -fsSL https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/scripts/setup-publish.sh -o /tmp/kmcp-setup-publish.sh
bash /tmp/kmcp-setup-publish.sh \
  --description "<プロジェクトの説明 (英語)>" \
  --group-id <GROUP_ID> \
  --version-ref <catalog のキー> --version <初期バージョン>   # gradle.properties の VERSION_NAME を使うなら省略
  # build-logic 等に置く場合: --convention-dir build-logic
```

- 必須は `--description` とバージョンの SSoT (`--version-ref` か `gradle.properties` の `VERSION_NAME`)。`--github-url` / `--license` / `--developer-id` / `--developer-name` / `--start-year` 等は未指定なら自動推定され、推定できない場合はエラーに「何が・なぜ・どう直すか」が表示されるので、それに従いオプションを追加して再実行する。オプション一覧は `--help`
- script がやること: (1) catalog に vanniktech プラグイン (Gradle / Kotlin の版から互換バージョンを自動選択) と自ライブラリのバージョンを冪等追記 (2) convention ビルド (`<convention-dir>/build.gradle.kts` / `settings.gradle.kts`) の生成。vanniktech と同じ classpath に KGP / AGP も載せる (3) `<convention-dir>/src/main/kotlin/publish-convention.gradle.kts` の生成（プレースホルダー置換済み）(4) `.github/workflows/publish.yml` / `publish-check.yml` の生成（runner は Apple ターゲットの有無で自動選択）
- 再実行しても安全（冪等）。既存ファイルは `--force` なしでは上書きしない
- stdout 末尾 1 行の JSON に結果が出る。`action_required` に項目がある場合（例: 既存の `buildSrc/build.gradle.kts` に依存を手動マージ）は、`https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/example/` 配下の該当ファイルを取得して **AI が手動でマージする**。「バージョン付きのプラグイン要求を置き換える」(buildSrc の時のみ) は指摘行の `alias(libs.plugins.X)` を `id(libs.plugins.X.get().pluginId)` に置き換える。`includeBuild` の追加はルート `settings.gradle.kts` の `pluginManagement { }` に追加する

## Step 3: 各モジュールに convention plugin を適用

**AI の責務**: 公開対象モジュールを判断し、各モジュールの `build.gradle.kts` に以下を追加する:

```kotlin
plugins {
    id("publish-convention")
}

group = "<GROUP_ID>"
version = libs.versions.<versionRef>.get()   // gradle.properties の GROUP / VERSION_NAME を使う場合は 2 行とも不要
```

モジュール固有の説明は `mavenPublishing { pom { description.set("...") } }` で上書きする。`coordinates(...)` は呼ばない (artifactId は convention が `project.path` から導出する)。
Dokka・古い Kotlin 向けの `languageVersion` 床などの任意設定は `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/references/convention-options.md` を取得して参照する。

## Step 4: ローカル動作確認

```bash
./gradlew publishToMavenLocal
```

署名はスキップされるので GPG 鍵が無くても通る。成功したら `~/.m2/repository/<group-path>/` にアーティファクトが生成されていることを確認する。失敗したら `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/references/troubleshooting.md` を取得して参照する。

## Step 5: GPG 鍵と GitHub Secrets のセットアップ (setup-secrets.sh)

```bash
curl -fsSL https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/scripts/setup-secrets.sh -o /tmp/kmcp-setup-secrets.sh
bash /tmp/kmcp-setup-secrets.sh
```

対話 script。GPG 鍵の生成（既存鍵は `--key-id`）→ `SIGNING_KEY_ID`（フィンガープリント末尾 8 桁）の抽出 → 公開鍵のキーサーバー送信 → 秘密鍵の ASCII armor export → `gh secret set` による 5 つの Secrets 登録までを自動化する。

- ユーザーの手作業は **Sonatype Central Portal の User Token 発行だけ**（script が手順を案内し、入力を促す）
- キーサーバー送信と Secrets 登録の前には確認プロンプトが出る。`--dry-run` で副作用なしの実行計画確認ができる
- script が使えない環境では、フォールバック手動手順として
  `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/references/gpg-setup.md` と
  `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/references/github-secrets.md` を取得して参照する

### 必要な GitHub Secrets (5つ)

| Secret 名 | 説明 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal ユーザートークンのユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal ユーザートークンのパスワード |
| `SIGNING_KEY_ID` | GPG 鍵の短縮 ID（フィンガープリント末尾 8 桁） |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `GPG_KEY_CONTENTS` | GPG 秘密鍵の ASCII armor 形式 |

## Step 6: 公開

バージョンの SSoT をリリース版に上げて main にマージし、tag `v<版>` で GitHub Release を publish する (pre-release も可)。publish ワークフローが tag とバージョンの一致・非 SNAPSHOT を検証して `publishAndReleaseToMavenCentral` を実行する。手動実行 (workflow_dispatch) は既定 `dry_run: true` で check と `publishToMavenLocal` だけ流れる。
代替フロー (tag push 駆動 + Release 自動生成) は `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/references/release-flow.md` を参照。
