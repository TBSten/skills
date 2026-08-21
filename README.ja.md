# Skills

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

TBSten の [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル・ルールコレクション。

> **ステータス:** 🌱 WIP ・ 🧪 Experimental ・ ✅ Active ・ 💎 Active-Prime ・ ❌ Archived

## ⭐️ 利用可能なスキル

<table>
<tr>
<th>グループ</th>
<th>スキル</th>
<th>インストール</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td>🔴 タスク管理</td>
<td><a href="./skills/local-ticket-system.ja.md">local-ticket-system</a></td>
<td>

```sh
gh skill install tbsten/skills local-ticket-system
```

</td>
<td>✅ Active</td>
<td>Markdown ベースのローカルチケット管理システム（task / bug / chapter 対応）</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/status-board.ja.md">status-board</a></td>
<td>

```sh
gh skill install tbsten/skills status-board
```

</td>
<td>🧪 Experimental</td>
<td>抱えている作業 (PR / issue / ローカルブランチ / 未決事項) を依存グラフ + カンバンの 1 枚 HTML に集約</td>
</tr>
<tr>
<td>🟢 Kotlin / Android アプリ開発</td>
<td><a href="./skills/kotlin-tuple.ja.md">kotlin-tuple</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-tuple
```

</td>
<td>✅ Active</td>
<td>Kotlin/KMP 向け型安全な Tuple ユーティリティ</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/simple-loader.ja.md">simple-loader</a></td>
<td>

```sh
gh skill install tbsten/skills simple-loader
```

</td>
<td>✅ Active</td>
<td>Kotlin/Compose Multiplatform 向け sealed interface ベースの非同期データ読み込み状態管理ステートマシン</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/navigation3-main-tab.ja.md">navigation3-main-tab</a></td>
<td>

```sh
gh skill install tbsten/skills navigation3-main-tab
```

</td>
<td>✅ Active</td>
<td>Navigation 3 の SceneStrategy を活用した下タブ管理パターン (KMP + Compose)</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kmp-snapshot-testing-setup.ja.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

</td>
<td>✅ Active</td>
<td>KMP + Compose プロジェクト向けスナップショットテスト基盤 (Kotest PBT + Turbine) のセットアップ</td>
</tr>
<tr>
<td>🟣 Kotlin ライブラリ/ツール開発</td>
<td><a href="./skills/kotlin-compiler-plugin-setup.ja.md">kotlin-compiler-plugin-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-setup
```

</td>
<td>✅ Active</td>
<td>Kotlin Compiler Plugin プロジェクト (buildSrc / kctfork / 統合テスト) のセットアップ</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kotlin-compiler-plugin-dev.ja.md">kotlin-compiler-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-dev
```

</td>
<td>✅ Active</td>
<td>30+ の既存プラグイン調査データをもとに Kotlin Compiler Plugin の開発・レビューを支援</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kotlin-maven-central-publish.ja.md">kotlin-maven-central-publish</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

</td>
<td>✅ Active</td>
<td>Kotlin/KMP 向け Maven Central 公開設定 (Vanniktech + GPG + GitHub Actions)</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/intellij-plugin-dev.ja.md">intellij-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills intellij-plugin-dev
```

</td>
<td>🌱 WIP</td>
<td>IntelliJ / Android Studio プラグインをエージェント主導で開発するためのツーリング・検証リファレンス</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/ksp-plugin-setup.ja.md">ksp-plugin-setup</a></td>
<td>

```sh
gh skill install tbsten/skills ksp-plugin-setup
```

</td>
<td>🧪 Experimental</td>
<td>KSP プラグインを 3 モジュール構成でスキャフォールド (kctfork golden + Konsist)</td>
</tr>
<tr>
<td>🔵 Web フロントエンド</td>
<td><a href="./skills/react-vite-supabase-starter.ja.md">react-vite-supabase-starter</a></td>
<td>

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

</td>
<td>✅ Active</td>
<td>React + Vite + Tailwind v4 + shadcn/ui + TanStack + Supabase の Web アプリをスキャフォールド</td>
</tr>
<tr>
<td>⚫️ Git / GitHub</td>
<td><a href="./skills/exploratory-pr-verification.ja.md">exploratory-pr-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

</td>
<td>✅ Active</td>
<td>Kotlin プロジェクトの PR を複数 subagent 並列で探索的に検証する運用規約</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/exploratory-nightly-verification.ja.md">exploratory-nightly-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

</td>
<td>✅ Active</td>
<td>nightly CI から main を 60 分 single-shot で探索検証し、発見を Markdown に書き出す</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/pr-fix-loop.ja.md">pr-fix-loop</a></td>
<td>

```sh
gh skill install tbsten/skills pr-fix-loop
```

</td>
<td>✅ Active</td>
<td>複数 PR を並行して green に。CI 失敗の分類・修正委譲、コメント対応、stacked PR の rebase 連鎖</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/github-get-attachment-url.ja.md">github-get-attachment-url</a></td>
<td>

```sh
gh skill install tbsten/skills github-get-attachment-url
```

</td>
<td>✅ Active</td>
<td>Issue を作らずにローカルファイルの user-attachments URL を取得 (Playwright ランナー同梱)</td>
</tr>
</table>

## 📝 利用可能なルール

<table>
<tr>
<th>グループ</th>
<th>ルール</th>
<th>インストール</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td>🟢 Kotlin / Android アプリ開発</td>
<td><a href="./rules/kmp-snapshot-testing.ja.md">kmp-snapshot-testing</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>✅ Active</td>
<td>Kotlin Multiplatform プロジェクト向けスナップショット PBT テストルール (Kotest + Turbine)</td>
</tr>
<tr>
<td></td>
<td><a href="./rules/kmp-error-handling.ja.md">kmp-error-handling</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-error-handling
```

</td>
<td>✅ Active</td>
<td>Kotlin Multiplatform + Compose プロジェクト向けエラーハンドリング・ワーニング検知ルール</td>
</tr>
<tr>
<td></td>
<td><a href="./rules/kmp-layered-architecture.ja.md">kmp-layered-architecture</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>✅ Active</td>
<td>Kotlin Multiplatform + Compose プロジェクト向け 4 層アーキテクチャ (App/UI/Domain/Data) ルール</td>
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

## 💬 利用可能なプロンプト

スキルをインストールせずに使える一回限りのプロンプト集。「実行」列のプロンプトをコピーして Claude Code に貼り付けると実行できます。

<table>
<tr>
<th>グループ</th>
<th>プロンプト</th>
<th>実行</th>
<th>ステータス</th>
<th>説明</th>
</tr>
<tr>
<td>🟢 Kotlin / Android アプリ開発</td>
<td><a href="./prompts/kmp-snapshot-testing-setup.ja.md">kmp-snapshot-testing-setup</a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kmp-snapshot-testing-setup/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>KMP + Compose 向けスナップショットテスト基盤のセットアップ (skill のプロンプト版)</td>
</tr>
<tr>
<td>🟣 Kotlin ライブラリ/ツール開発</td>
<td><a href="./prompts/kotlin-compiler-plugin-setup.ja.md">kotlin-compiler-plugin-setup</a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-compiler-plugin-setup/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>Kotlin Compiler Plugin プロジェクトのセットアップ (skill のプロンプト版)</td>
</tr>
<tr>
<td></td>
<td><a href="./prompts/kotlin-maven-central-publish.ja.md">kotlin-maven-central-publish</a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-maven-central-publish/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>Kotlin/KMP 向け Maven Central 公開設定 (skill のプロンプト版)</td>
</tr>
<tr>
<td></td>
<td><a href="./prompts/ksp-plugin-setup.ja.md">ksp-plugin-setup</a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/ksp-plugin-setup/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>KSP プラグインプロジェクトをスキャフォールド (skill のプロンプト版)</td>
</tr>
<tr>
<td>🔵 Web フロントエンド</td>
<td><a href="./prompts/react-vite-supabase-starter.ja.md">react-vite-supabase-starter</a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/react-vite-supabase-starter/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>React + Vite + Supabase の Web アプリをスキャフォールド (skill のプロンプト版)</td>
</tr>
</table>

## 🤝 スキル / ルールのコントリビュート

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
<td>✅ Active</td>
<td>プロジェクトの知見をスキルとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.ja.md">contribute-rule</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-rule
```

</td>
<td>✅ Active</td>
<td>プロジェクトの知見をルールとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
<tr>
<td><a href="./skills/contribute-prompt.ja.md">contribute-prompt</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-prompt
```

</td>
<td>🧪 Experimental</td>
<td>プロジェクトの知見をプロンプトとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
<tr>
<td><a href="./skills/contribute-batch.ja.md">contribute-batch</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-batch
```

</td>
<td>🧪 Experimental</td>
<td>複数の知見を skill / rule / prompt に仕分けし TBSten/skills に 1 つの PR を作成</td>
</tr>
</table>
