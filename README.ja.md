# Skills

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

TBSten の [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル・ルールコレクション。

## 利用可能なスキル

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
npx skills add tbsten/skills --skill kotlin-tuple
```

</td>
<td>Kotlin/KMP 向け型安全な Tuple ユーティリティ (Tuple0–Tuple20)</td>
<td><a href="./kotlin-tuple.ja.md">kotlin-tuple.ja.md</a></td>
</tr>
<tr>
<td>contribute-skill</td>
<td>

```sh
npx skills add tbsten/skills --skill contribute-skill
```

</td>
<td>プロジェクトの知見をスキルとしてパッケージし TBSten/skills に PR を作成</td>
<td><a href="./contribute-skill.ja.md">contribute-skill.ja.md</a></td>
</tr>
<tr>
<td>contribute-rule</td>
<td>

```sh
npx skills add tbsten/skills --skill contribute-rule
```

</td>
<td>プロジェクトの知見をルールとしてパッケージし TBSten/skills に PR を作成</td>
<td><a href="./contribute-rule.ja.md">contribute-rule.ja.md</a></td>
</tr>
<tr>
<td>simple-loader</td>
<td>

```sh
npx skills add tbsten/skills --skill simple-loader
```

</td>
<td>Kotlin/Compose Multiplatform 向け sealed interface ベースの非同期データ読み込み状態管理ステートマシン</td>
<td><a href="./simple-loader.ja.md">simple-loader.ja.md</a></td>
</tr>
</table>

## 利用可能なルール

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
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-layered-architecture
```

</td>
<td>Kotlin Multiplatform + Compose プロジェクト向け 4 層アーキテクチャ (App/UI/Domain/Data) ルール</td>
<td><a href="./rules/kmp-layered-architecture/RULE.md">RULE.md</a></td>
</tr>
<tr>
<td>kmp-snapshot-testing</td>
<td>

```sh
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-snapshot-testing
```

</td>
<td>Kotlin Multiplatform プロジェクト向けスナップショット PBT テストルール (Kotest + Turbine)</td>
<td><a href="./rules/kmp-snapshot-testing/RULE.md">RULE.md</a></td>
</tr>
</table>
