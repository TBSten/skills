# Skills

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

TBSten の [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル・ルールコレクション。

## ⭐️ 利用可能なスキル

<table>
<tr>
<th>スキル</th>
<th>インストール</th>
<th>説明</th>
</tr>
<tr>
<td><a href="./skills/local-ticket-system.ja.md">local-ticket-system</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill local-ticket-system
```

</td>
<td>Markdown ベースのローカルチケット管理システム（task / bug / chapter 対応）</td>
</tr>
<tr>
<td><a href="./skills/kotlin-tuple.ja.md">kotlin-tuple</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-tuple
```

</td>
<td>Kotlin/KMP 向け型安全な Tuple ユーティリティ</td>
</tr>
<tr>
<td><a href="./skills/simple-loader.ja.md">simple-loader</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill simple-loader
```

</td>
<td>Kotlin/Compose Multiplatform 向け sealed interface ベースの非同期データ読み込み状態管理ステートマシン</td>
</tr>
<tr>
<td><a href="./skills/navigation3-main-tab.ja.md">navigation3-main-tab</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill navigation3-main-tab
```

</td>
<td>Navigation 3 の SceneStrategy を活用した下タブ管理パターン (KMP + Compose)</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-setup.ja.md">kotlin-compiler-plugin-setup</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-compiler-plugin-setup
```

</td>
<td>Kotlin Compiler Plugin のマルチモジュールプロジェクト (buildSrc、ユニットテスト kctfork、インテグレーションテスト) のセットアップ</td>
</tr>
<tr>
<td><a href="./skills/kotlin-maven-central-publish.ja.md">kotlin-maven-central-publish</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-maven-central-publish
```

</td>
<td>Kotlin/KMP プロジェクト向け Maven Central 公開設定（Vanniktech Maven Publish + GPG 署名 + GitHub Actions）</td>
</tr>
<tr>
<td><a href="./skills/kmp-snapshot-testing-setup.ja.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kmp-snapshot-testing-setup
```

</td>
<td>KMP + Compose プロジェクト向けスナップショットテスト基盤 (Kotest PBT + Turbine) のセットアップ</td>
</tr>
<tr>
<td><a href="./skills/react-vite-supabase-starter.ja.md">react-vite-supabase-starter</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill react-vite-supabase-starter
```

</td>
<td>React + Vite + TypeScript + Tailwind v4 + shadcn/ui + TanStack Router/Query + Supabase の Web アプリをスキャフォールド</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-dev.ja.md">kotlin-compiler-plugin-dev</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-compiler-plugin-dev
```

</td>
<td>30+ の既存プラグイン調査データをもとに Kotlin Compiler Plugin の開発・レビューを支援する（Extension Point 選択、設計パターン、前例調査）</td>
</tr>
<tr>
<td><a href="./skills/add-support-kotlin-version.ja.md">add-support-kotlin-version</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill add-support-kotlin-version
```

</td>
<td>Kotlin Compiler Plugin プロジェクトのサポート対象 Kotlin バージョンを追加・削除する（compat module layer / source set separation 対応）</td>
</tr>
<tr>
<td><a href="./skills/android-convention-plugins.ja.md">android-convention-plugins</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill android-convention-plugins
```

</td>
<td>「Primitive & Module」構成を採用した、Android プロジェクト向けの拡張性の高い Gradle Convention Plugins 構成</td>
</tr>
</table>

## 📝 利用可能なルール

<table>
<tr>
<th>ルール</th>
<th>インストール</th>
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
<th>説明</th>
</tr>
<tr>
<td><a href="./skills/contribute-skill.ja.md">contribute-skill</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-skill
```

</td>
<td>プロジェクトの知見をスキルとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.ja.md">contribute-rule</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-rule
```

</td>
<td>プロジェクトの知見をルールとしてパッケージし TBSten/skills に PR を作成</td>
</tr>
</table>
