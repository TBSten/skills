---
name: ksp-plugin-setup
description: >
  KSP plugin (Symbol Processor) プロジェクトを cream.kt 由来の構成で一式セットアップする。
  runtime (KMP 全ターゲット、アノテーション宣言のみ) / ksp (JVM only, processor) / test (KMP 統合テスト)
  の 3 モジュール、feature / core / options / util の 4 層 + ProcessContext + context parameters による
  processor 設計、kctfork による e2e コンパイルテスト・facet 形式の Markdown golden・generator 駆動の
  snapshot matrix・Konsist によるレイヤ自動強制、version catalog / buildLogic / KSP×KMP workaround /
  GitHub Actions matrix CI を含む。生成先プロジェクトに .claude/rules/*.md を配置して規約を常設する。
  Use when requested: "KSP プラグインを作りたい", "KSP processor のプロジェクトをセットアップ",
  "setup ksp plugin", "cream と同じ構成で KSP プラグインを作って", "symbol processor のプロジェクト構成",
  "KSP プラグインのテスト基盤を整えたい", "アノテーションプロセッサを新規作成".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

# KSP Plugin Setup

KSP plugin プロジェクトを、実運用に到達した構成 ([cream.kt](https://github.com/TBSten/cream) 由来)
で一式セットアップする。スキャフォールドするだけでなく、生成先に `.claude/rules/*.md` を置いて
以後も構成が崩れないようにする。

コピー / プレースホルダー置換 / rename / META-INF 配置などの機械的な作業はすべて
`scripts/scaffold.sh` が行う。**AI の責務は、確認事項のヒアリング・script の実行・出力のレビュー・
ビルド確認・golden のレビューだけ**。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト名** — ルートプロジェクト名 (kebab-case)。モジュール名 `<project-name>-runtime` /
   `<project-name>-ksp` の接頭辞になる
2. **パッケージ名 / Group ID** — 例: `com.example.myplugin`
3. **最初のアノテーション名** — 例: `@Greeting`。feature ディレクトリ名の由来になる
4. **セットアップ範囲** — 以下から選択 (デフォルト: 全て)
   - [x] Gradle 基盤 + runtime / ksp モジュール + テスト基盤 (常にセット)
   - [x] test モジュール (KMP 統合テスト) — 外すなら `--skip-test-module`
   - [x] CI (GitHub Actions matrix) — 外すなら `--skip-ci`
   - [x] `.claude/rules/` (規約の常設) — 外すなら `--skip-rules`
5. **Kotlin / KSP バージョン** — デフォルトは `example/gradle/libs.versions.toml` の値。
   変えるなら `--kotlin-version` / `--ksp-version` (KSP は `<kotlin>-<ksp>` 形式のフル文字列)

### Step 1: scaffold script の実行

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/scaffold.sh" \
  --dest <生成先ディレクトリ> \
  --name <project-name> \
  --package <パッケージ名> \
  --annotation <アノテーション名 (PascalCase)>
```

**script は読解・書き換え・再実装せず、そのまま実行する。**
コピー対象・ディレクトリ再マッピング・プレースホルダー置換・rename の詳細仕様は
script 自身 (と冒頭コメント) が SSoT。script が行うことの要約:

- `example/` 一式を再マッピングしてコピーする (ソースセット / パッケージパスのディレクトリ生成込み)
- プレースホルダーを置換する: プロジェクト名 / groupId / パッケージ /
  `Example` → プロジェクト名の PascalCase / `Greeting`・`greeting`・`greetingFun` →
  アノテーション名のケース派生 / `<owner>`・`<repo>`・`<year>`
- `Greeting*` / `Example*` 系のファイル・ディレクトリを rename する
- `META-INF/services/` に provider の FQN を配置する
- `assets/rules/` の 5 ファイルを生成先の `.claude/rules/` に配置する (`--skip-rules` で省略)

任意オプション: `--group-id` (省略時は `--package` と同じ) / `--owner` `--repo` (省略時は
`--dest` の git remote origin から推定。推定できないとエラーになるのでユーザーに確認して渡す) /
`--kotlin-version` `--ksp-version` / `--skip-ci` `--skip-rules` `--skip-test-module` /
`--dry-run` (書き込まず配置予定だけ出力) / `--force` (既存ファイルの上書きを許可。
デフォルトでは既存ファイルがあるとエラーで止まる)。

実行が終わったら、**出力された配置ファイル一覧をレビューする**。事前に確認したい場合は
`--dry-run` を先に実行する。

### Step 2: 生成物レビュー

配置一覧と主要ファイルを見て、以下の観点を確認する。設計判断の背景は references を参照:

- **runtime はアノテーション宣言のみ** (実行時ロジック 0 行、KMP 全ターゲット + `explicitApi()`)。
  API 設計の規約は references/processor-design.md
- **ksp モジュールの層**: root 直下 3 ファイルのみ / 依存は feature → core → util の一方向
  (唯一の上向きは feature → ProcessContext) / feature 間依存禁止 / `feature/`・`core/` 直下に
  `.kt` を置かない / 層ごとに context を絞る
- **version catalog がすべての SSoT**。KSP のバージョンは `<kotlin>-<ksp>` 形式で Kotlin とセットで
  上げる。foojay resolver は root と buildLogic の両方に必要。詳細は references/build-and-ci.md
- **KSP × KMP workaround** (test モジュール): `kspCommonMainKotlinMetadata` の生成物を commonMain の
  srcDir に足す。ただし `*Test` の ksp タスクは無効化しない (kotest の per-target launcher に必要)
- **テスト基盤**: kctfork e2e / facet 形式 Markdown golden / generator 駆動 snapshot / 診断 golden /
  Konsist。各ファイルの役割は references/testing.md
- **`.claude/rules/`** は Konsist テストと対。依存方向テーブルを変えたら同じコミットで ArchTest も
  更新する旨を、プロジェクトの CLAUDE.md にも書いておく

publish が必要な場合、GPG 鍵・secrets・Sonatype 登録まで含む完全な手順は
**`kotlin-maven-central-publish` スキル**に委譲する。

### Step 3: ビルド確認

gradle wrapper が無ければ先に生成する (`gradle wrapper`)。その後:

```sh
bash "${CLAUDE_SKILL_DIR}/scripts/verify.sh" --project-dir <生成先> --fresh
```

verify.sh は SKILL の 4 コマンド (`:<name>-ksp:test` / golden 記録 / `jvmTest` / `ktlintCheck`) を
順に実行し、ログを `<生成先>/.local/tmp/` に保存して SUCCESS / FAILED サマリを出す。
`--fresh` は scaffold 直後用で、golden 記録を先に実行する。

golden の初回記録後は **必ず中身を読んでからコミットする**。最初の記録が誤った出力を捕まえる
唯一の機会で、以降は差分しか見えなくなる。

## リソース

| リソース | いつ読むか |
|---|---|
| scripts/scaffold.sh | Step 1 で実行する (読解・改変はしない)。仕様の SSoT |
| scripts/verify.sh | Step 3 で実行する |
| references/build-and-ci.md | Gradle 基盤・KSP×KMP workaround・kotest 配線・CI・publish の詳細と落とし穴 |
| references/processor-design.md | example を自分のアノテーションに置き換える時、生成 / 診断 / option の設計判断 |
| references/testing.md | テスト基盤の各ファイルの役割、feature のテストを追加する時、Konsist の注意点 |
| assets/rules/*.md | scaffold.sh が生成先の `.claude/rules/` にコピーするテンプレート (規約の SSoT) |
| example/ | scaffold.sh のコピー元一式 (直接編集・手動コピーはしない) |

## セットアップ完了メッセージ

```
## セットアップ完了

### モジュール構成
- <project-name>-runtime/ (KMP 全ターゲット、アノテーション宣言のみ)
- <project-name>-ksp/ (JVM only、feature / core / options / util)
- test/ (KMP 統合テスト)

### テスト基盤
- kctfork e2e / facet golden / generator 駆動 snapshot / Konsist アーキテクチャテスト

### 規約
- .claude/rules/ に 5 ファイル配置済み

### ビルド結果
- :<project-name>-ksp:test: [SUCCESS / FAILED]
- jvmTest: [SUCCESS / FAILED]
- ktlintCheck: [SUCCESS / FAILED]

### 次のステップ
1. runtime に自分のアノテーションを宣言する
2. core/common に <Name>SourceAnnotation を追加する
3. feature/<name>/Process<Name>.kt を書き、SymbolProcessor に dispatch を 1 行足す
4. feature の 5 種テストを追加し、golden を記録する
```
