# KSP Plugin プロジェクトセットアップ

このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/ksp-plugin-setup` として配布されている一回限りのプロンプト。KSP plugin (Symbol Processor) プロジェクトを、実運用に到達した構成 ([cream.kt](https://github.com/TBSten/cream) 由来) で一式セットアップする。スキャフォールドするだけでなく、生成先に `.claude/rules/*.md` を置いて以後も構成が崩れないようにする。

コピー / プレースホルダー置換 / rename / META-INF 配置などの機械的な作業はすべて同梱の `scripts/scaffold.sh` が行う。**AI の責務は、確認事項のヒアリング・script の実行・出力のレビュー・ビルド確認・golden のレビューだけ**。

## 参照ファイルの取得方法

このプロンプトが参照するファイルは GitHub リポジトリ [TBSten/skills](https://github.com/TBSten/skills) にある。scaffold script が `example/` / `assets/` 一式をローカル参照するので、**最初に必ず sparse clone する**:

```sh
git clone --depth 1 --filter=blob:none --sparse https://github.com/TBSten/skills.git /tmp/tbsten-skills
git -C /tmp/tbsten-skills sparse-checkout set skills/ksp-plugin-setup
```

以降に登場する `scripts/...` / `references/...` は、すべてリポジトリ内の `skills/ksp-plugin-setup/` 配下のパスを指す (ローカルでは `/tmp/tbsten-skills/skills/ksp-plugin-setup/` 配下)。

## 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト名** — ルートプロジェクト名 (kebab-case)。モジュール名 `<project-name>-runtime` / `<project-name>-ksp` の接頭辞になる
2. **パッケージ名 / Group ID** — 例: `com.example.myplugin`
3. **最初のアノテーション名** — 例: `@Greeting`。feature ディレクトリ名の由来になる
4. **セットアップ範囲** — デフォルトは全て。test モジュールを外すなら `--skip-test-module`、CI を外すなら `--skip-ci`、`.claude/rules/` を外すなら `--skip-rules`
5. **Kotlin / KSP バージョン** — デフォルトは example の version catalog の値。変えるなら `--kotlin-version` / `--ksp-version` (KSP は `<kotlin>-<ksp>` 形式のフル文字列)

## セットアップ手順

### Step 1: scaffold script の実行

```sh
bash /tmp/tbsten-skills/skills/ksp-plugin-setup/scripts/scaffold.sh \
  --dest <生成先ディレクトリ> \
  --name <project-name> \
  --package <パッケージ名> \
  --annotation <アノテーション名 (PascalCase)>
```

**script は読解・書き換え・再実装せず、そのまま実行する。** コピー対象・ディレクトリ再マッピング・プレースホルダー置換・rename の詳細仕様は script 自身が SSoT。script が行うことの要約:

- `example/` 一式を再マッピングしてコピーする (ソースセット / パッケージパスのディレクトリ生成込み)
- プレースホルダーを置換する: プロジェクト名 / groupId / パッケージ / `Example` → プロジェクト名の PascalCase / `Greeting`・`greeting`・`greetingFun` → アノテーション名のケース派生 / `<owner>`・`<repo>`・`<year>`
- `Greeting*` / `Example*` 系のファイル・ディレクトリを rename する
- `META-INF/services/` に provider の FQN を配置する
- `assets/rules/` の 5 ファイルを生成先の `.claude/rules/` に配置する (`--skip-rules` で省略)

任意オプション: `--group-id` (省略時は `--package` と同じ) / `--owner` `--repo` (省略時は `--dest` の git remote origin から推定。推定できないとエラーになるのでユーザーに確認して渡す) / `--kotlin-version` `--ksp-version` / `--skip-ci` `--skip-rules` `--skip-test-module` / `--dry-run` (書き込まず配置予定だけ出力) / `--force` (既存ファイルの上書きを許可。デフォルトでは既存ファイルがあるとエラーで止まる)。

実行が終わったら、**出力された配置ファイル一覧をレビューする**。事前に確認したい場合は `--dry-run` を先に実行する。

### Step 2: 生成物レビュー

配置一覧と主要ファイルを見て、以下の観点を確認する:

- **runtime はアノテーション宣言のみ** (実行時ロジック 0 行、KMP 全ターゲット + `explicitApi()`)。API 設計の規約は [`references/processor-design.md`](https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/ksp-plugin-setup/references/processor-design.md)
- **ksp モジュールの層**: root 直下 3 ファイルのみ / 依存は feature → core → util の一方向 (唯一の上向きは feature → ProcessContext) / feature 間依存禁止 / `feature/`・`core/` 直下に `.kt` を置かない / 層ごとに context を絞る
- **version catalog がすべての SSoT**。KSP のバージョンは `<kotlin>-<ksp>` 形式で Kotlin とセットで上げる。foojay resolver は root と buildLogic の両方に必要。詳細は [`references/build-and-ci.md`](https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/ksp-plugin-setup/references/build-and-ci.md)
- **KSP × KMP workaround** (test モジュール): `kspCommonMainKotlinMetadata` の生成物を commonMain の srcDir に足す。ただし `*Test` の ksp タスクは無効化しない (kotest の per-target launcher に必要)
- **テスト基盤**: kctfork e2e / facet 形式 Markdown golden / generator 駆動 snapshot / 診断 golden / Konsist。各ファイルの役割は [`references/testing.md`](https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/ksp-plugin-setup/references/testing.md)
- **`.claude/rules/`** は Konsist テストと対。依存方向テーブルを変えたら同じコミットで ArchTest も更新する旨を、プロジェクトの CLAUDE.md にも書いておく

publish が必要な場合、GPG 鍵・secrets・Sonatype 登録まで含む完全な手順は [`kotlin-maven-central-publish` プロンプト](https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-maven-central-publish/PROMPT.md) に委譲する。

### Step 3: ビルド確認

gradle wrapper が無ければ先に生成する (`gradle wrapper`)。その後:

```sh
bash /tmp/tbsten-skills/skills/ksp-plugin-setup/scripts/verify.sh --project-dir <生成先> --fresh
```

verify.sh は 4 コマンド (`:<name>-ksp:test` / golden 記録 / `jvmTest` / `ktlintCheck`) を順に実行し、ログを `<生成先>/.local/tmp/` に保存して SUCCESS / FAILED サマリを出す。`--fresh` は scaffold 直後用で、golden 記録を先に実行する。

golden の初回記録後は **必ず中身を読んでからコミットする**。最初の記録が誤った出力を捕まえる唯一の機会で、以降は差分しか見えなくなる。

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
