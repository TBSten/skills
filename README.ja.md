# Skills

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

TBSten の [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル・ルールコレクション。

> **ステータス:** 🌱 WIP ・ 🧪 Experimental ・ 🟢 Active ・ 💎 Active-Prime ・ ❌ Archived

## ⭐️ 利用可能なスキル

<table>
<tr>
<th>スキル</th>
<th>インストール</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td><a href="./skills/local-ticket-system.ja.md">local-ticket-system</a></td>
<td>

```sh
gh skill install tbsten/skills local-ticket-system
```

</td>
<td>🟢 Active</td>
<td>Markdown ベースのローカルチケット管理システム（task / bug / chapter 対応）</td>
</tr>
<tr>
<td><a href="./skills/kotlin-tuple.ja.md">kotlin-tuple</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-tuple
```

</td>
<td>🟢 Active</td>
<td>Kotlin/KMP 向け型安全な Tuple ユーティリティ</td>
</tr>
<tr>
<td><a href="./skills/simple-loader.ja.md">simple-loader</a></td>
<td>

```sh
gh skill install tbsten/skills simple-loader
```

</td>
<td>🟢 Active</td>
<td>Kotlin/Compose Multiplatform 向け sealed interface ベースの非同期データ読み込み状態管理ステートマシン</td>
</tr>
<tr>
<td><a href="./skills/navigation3-main-tab.ja.md">navigation3-main-tab</a></td>
<td>

```sh
gh skill install tbsten/skills navigation3-main-tab
```

</td>
<td>🟢 Active</td>
<td>Navigation 3 の SceneStrategy を活用した下タブ管理パターン (KMP + Compose)</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-setup.ja.md">kotlin-compiler-plugin-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-setup
```

</td>
<td>🟢 Active</td>
<td>Kotlin Compiler Plugin のマルチモジュールプロジェクト (buildSrc、ユニットテスト kctfork、インテグレーションテスト) のセットアップ</td>
</tr>
<tr>
<td><a href="./skills/kotlin-maven-central-publish.ja.md">kotlin-maven-central-publish</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

</td>
<td>🟢 Active</td>
<td>Kotlin/KMP プロジェクト向け Maven Central 公開設定（Vanniktech Maven Publish + GPG 署名 + GitHub Actions）</td>
</tr>
<tr>
<td><a href="./skills/kmp-snapshot-testing-setup.ja.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

</td>
<td>🟢 Active</td>
<td>KMP + Compose プロジェクト向けスナップショットテスト基盤 (Kotest PBT + Turbine) のセットアップ</td>
</tr>
<tr>
<td><a href="./skills/react-vite-supabase-starter.ja.md">react-vite-supabase-starter</a></td>
<td>

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

</td>
<td>🟢 Active</td>
<td>React + Vite + TypeScript + Tailwind v4 + shadcn/ui + TanStack Router/Query + Supabase の Web アプリをスキャフォールド</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-dev.ja.md">kotlin-compiler-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-dev
```

</td>
<td>🟢 Active</td>
<td>30+ の既存プラグイン調査データをもとに Kotlin Compiler Plugin の開発・レビューを支援する（Extension Point 選択、設計パターン、前例調査、compat module layer / source set separation でのサポート Kotlin バージョン追加・削除も含む）</td>
</tr>
<tr>
<td><a href="./skills/exploratory-pr-verification.ja.md">exploratory-pr-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

</td>
<td>🟢 Active</td>
<td>Kotlin プロジェクトの PR を複数 subagent 並列で探索的に検証する運用規約 (PDCA / MCP / ticket 起票 / PR コメント policy / loop 終了処理)</td>
</tr>
<tr>
<td><a href="./skills/exploratory-nightly-verification.ja.md">exploratory-nightly-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

</td>
<td>🟢 Active</td>
<td>nightly CI ジョブから Kotlin プロジェクトの main を 60 分 single-shot で探索的検証。 発見を Markdown ファイルに逐次書き出し、 PR への副作用は一切なし</td>
</tr>
<tr>
<td><a href="./skills/pr-fix-loop.ja.md">pr-fix-loop</a></td>
<td>

```sh
gh skill install tbsten/skills pr-fix-loop
```

</td>
<td>🟢 Active</td>
<td>複数の GitHub PR を並行して green に。 1 パスで失敗 CI check (transient / lint / binary-compat / build / test) を分類して fix-ci-* skill に委譲、 review / issue コメントを取得〜resolve まで対応、 stacked PR の rebase を連鎖。 /loop driver と組み合わせて無人実行</td>
</tr>
<tr>
<td><a href="./skills/github-get-attachment-url.ja.md">github-get-attachment-url</a></td>
<td>

```sh
gh skill install tbsten/skills github-get-attachment-url
```

</td>
<td>🟢 Active</td>
<td>ローカルファイルを GitHub にアップロードし、Issue を作成せずに user-attachments URL(または Markdown)を取得する。同梱の決定的な Python + Playwright ランナーで実行</td>
</tr>
<tr>
<td><a href="./skills/intellij-plugin-dev.ja.md">intellij-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills intellij-plugin-dev
```

</td>
<td>🌱 WIP</td>
<td>IntelliJ Platform / Android Studio 向けプラグインをエージェント主導で開発するためのツーリング・進め方リファレンス。検証を 6 チャネルに分解し、ヘッドレス機能テスト (Kotlin Analysis API) と headless PNG 自己目視 (renderComposeScene + Jewel) の 2 本を主軸に据える。tool window・gutter line marker・エディタ追従・PSI 挿入 (コード生成)・VRT golden・Driver スモーク・build/since-until 配線をカバー</td>
</tr>
<tr>
<td><a href="./skills/status-board.ja.md">status-board</a></td>
<td>

```sh
gh skill install tbsten/skills status-board
```

</td>
<td>🧪 Experimental</td>
<td>抱えている作業 (GitHub の PR / issue、 ローカルブランチ、 会話の中にしか無い未決事項) を 1 枚の standalone HTML に。 依存グラフの SVG + エピック別カンバンで、 「人間待ち」 と 「次の一手」 が一目で分かる。 確認したいことはブラウザ上の textarea で回答を受け取れる。 GraphQL 1 往復 / 既定値の作り込み / 生成物への自己検証同梱 で速度を作っている</td>
</tr>
</table>

## 📝 利用可能なルール

<table>
<tr>
<th>ルール</th>
<th>インストール</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td><a href="./rules/kmp-layered-architecture.ja.md">kmp-layered-architecture</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>🟢 Active</td>
<td>Kotlin Multiplatform + Compose プロジェクト向け 4 層アーキテクチャ (App/UI/Domain/Data) ルール</td>
</tr>
<tr>
<td><a href="./rules/kmp-snapshot-testing.ja.md">kmp-snapshot-testing</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>🟢 Active</td>
<td>Kotlin Multiplatform プロジェクト向けスナップショット PBT テストルール (Kotest + Turbine)</td>
</tr>
<tr>
<td><a href="./rules/kmp-error-handling.ja.md">kmp-error-handling</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-error-handling
```

</td>
<td>🟢 Active</td>
<td>Kotlin Multiplatform + Compose プロジェクト向けエラーハンドリング・ワーニング検知ルール</td>
</tr>
</table>

<details>

<summary> ルールのインストール方法 </summary>

ルールは `rules/install.sh` 経由でインストールします。`RULE.md` を `.claude/rules/` に、参照ファイルをカレントディレクトリにダウンロードします。

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- <rule-name>
```

#### オプション

| オプション | 説明 |
|---|---|
| `as=<name>` | デフォルト名の代わりに `.claude/rules/<name>.md` として保存 |
| `--ref=<ref>` or `-r=<ref>` | ダウンロード元の Git ref (ブランチ名、タグ、コミットハッシュ)。デフォルト: `main` |

#### 例

```sh
# カスタム名でインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-layered-architecture as=my-architecture

# 特定ブランチからインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing --ref=feature/new-rule

# 特定コミットからインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing -r=abc1234
```

</details>

## 🤝 スキル / ルールの貢献

以下の skills を使用してこのリポジトリへの Pull Request を作成してください。

<table>
<tr>
<th>スキル</th>
<th>インストール</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td><a href="./skills/contribute-skill.ja.md">contribute-skill</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-skill
```

</td>
<td>🟢 Active</td>
<td>プロジェクトの知見をスキルとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.ja.md">contribute-rule</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-rule
```

</td>
<td>🟢 Active</td>
<td>プロジェクトの知見をルールとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
</table>
