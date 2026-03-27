# Skills

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

TBSten の [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル・ルールコレクション。

## ⭐️ 利用可能なスキル

<table>
<tr>
<th>スキル</th>
<th>インストール</th>
<th>説明</th>
<th>詳細</th>
</tr>
<tr>
<td>kotlin-tuple</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-tuple
```

</td>
<td>Kotlin/KMP 向け型安全な Tuple ユーティリティ</td>
<td><a href="./skills/kotlin-tuple.ja.md">詳細</a></td>
</tr>
<tr>
<td>contribute-skill</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-skill
```

</td>
<td>プロジェクトの知見をスキルとしてパッケージし TBSten/skills に PR を作成</td>
<td><a href="./skills/contribute-skill.ja.md">詳細</a></td>
</tr>
<tr>
<td>contribute-rule</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-rule
```

</td>
<td>プロジェクトの知見をルールとしてパッケージし TBSten/skills に PR を作成</td>
<td><a href="./skills/contribute-rule.ja.md">詳細</a></td>
</tr>
<tr>
<td>simple-loader</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill simple-loader
```

</td>
<td>Kotlin/Compose Multiplatform 向け sealed interface ベースの非同期データ読み込み状態管理ステートマシン</td>
<td><a href="./skills/simple-loader.ja.md">詳細</a></td>
</tr>
<tr>
<td>kmp-snapshot-testing-setup</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kmp-snapshot-testing-setup
```

</td>
<td>KMP + Compose プロジェクト向けスナップショットテスト基盤 (Kotest PBT + Turbine) のセットアップ</td>
<td><a href="./skills/kmp-snapshot-testing-setup.ja.md">詳細</a></td>
</tr>
<tr>
<td>navigation3-main-tab</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill navigation3-main-tab
```

</td>
<td>Navigation 3 の SceneStrategy を活用した下タブ管理パターン (KMP + Compose)</td>
<td><a href="./skills/navigation3-main-tab.ja.md">詳細</a></td>
</tr>
<tr>
<td>local-ticket-system</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill local-ticket-system
```

</td>
<td>Markdown ベースのローカルチケット管理システム</td>
<td><a href="./skills/local-ticket-system.ja.md">詳細</a></td>
</tr>
<tr>
<td>kotlin-maven-central-publish</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-maven-central-publish
```

</td>
<td>Kotlin/KMP プロジェクト向け Maven Central 公開設定（Vanniktech Maven Publish + GPG 署名 + GitHub Actions）</td>
<td><a href="./skills/kotlin-maven-central-publish.ja.md">詳細</a></td>
</tr>
</table>

## 📝 利用可能なルール

<table>
<tr>
<th>ルール</th>
<th>インストール</th>
<th>説明</th>
<th>詳細</th>
</tr>
<tr>
<td>kmp-layered-architecture</td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>Kotlin Multiplatform + Compose プロジェクト向け 4 層アーキテクチャ (App/UI/Domain/Data) ルール</td>
<td><a href="./rules/kmp-layered-architecture.ja.md">詳細</a></td>
</tr>
<tr>
<td>kmp-snapshot-testing</td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>Kotlin Multiplatform プロジェクト向けスナップショット PBT テストルール (Kotest + Turbine)</td>
<td><a href="./rules/kmp-snapshot-testing.ja.md">詳細</a></td>
</tr>
</table>

## ルールのインストール方法

ルールは `rules/install.sh` 経由でインストールします。`RULE.md` を `.claude/rules/` に、参照ファイルをカレントディレクトリにダウンロードします。

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- <rule-name>
```

### オプション

| オプション | 説明 |
|---|---|
| `as=<name>` | デフォルト名の代わりに `.claude/rules/<name>.md` として保存 |
| `--ref=<ref>` or `-r=<ref>` | ダウンロード元の Git ref (ブランチ名、タグ、コミットハッシュ)。デフォルト: `main` |

### 例

```sh
# カスタム名でインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-layered-architecture as=my-architecture

# 特定ブランチからインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing --ref=feature/new-rule

# 特定コミットからインストール
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing -r=abc1234
```
