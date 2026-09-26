---
name: bump-library-version
description: >
  example-lib のバージョンを上げる。SSoT は gradle/libs.versions.toml の `example-lib = "X.Y.Z"`。
  scripts/bump-version.sh が SSoT と許可リスト (README / docs) 内の Maven 座標を一括で書き換え、
  取りこぼしを defensive grep で洗い出す。「バージョン上げて」「N.N.N にして」「bump to X.Y.Z」
  「次の patch」「minor bump」「リリース準備」と言われたら必ずこの skill を使う。
  手で sed すると README や docs に古いバージョンが残るので使わないこと。
---

# Bump Library Version

example-lib (`<group-id>:example-lib-*`) のバージョンを上げる。

決定的な作業 (書き換え・取りこぼし検出) は `scripts/bump-version.sh` がやる。
**スクリプトを読解・書き換え・再実装せず、そのまま実行する。** AI の仕事は
「目標バージョンを決める」「REVIEW 行を判断する」「差分を確認して commit する」の 3 つだけ。

## Step 1. 目標バージョンを決める

```bash
grep -E '^example-lib[[:space:]]*=' gradle/libs.versions.toml
```

ユーザーの発話から目標を決める:

| 発話 | 目標 (現在 0.1.5 のとき) |
|---|---|
| 「0.1.6 にして」 | `0.1.6` |
| 「次の patch」 | `0.1.6` |
| 「minor bump」 | `0.2.0` |
| 「major bump」「1.0 にして」 | `1.0.0` |
| 「alpha / beta / rc にして」 | 形式 (`1.0.0-alpha01` / `1.0.0-beta.1` 等) を過去のリリースに合わせる。分からなければ聞く |

曖昧なら推測せず**聞く**。過去の形式は `gh release list --limit 5` で確認できる。

## Step 2. dry-run で計画を見る

```bash
bash .claude/skills/bump-library-version/scripts/bump-version.sh <version> --dry-run
```

書き換え予定のファイルと件数が出る。ユーザーへの確認は省略してよい (情報提供として見せる)。

## Step 3. 実行する

```bash
bash .claude/skills/bump-library-version/scripts/bump-version.sh <version>
```

- `OK ...` で終われば成功。`ERROR:` なら文面の指示に従う (バージョン形式・catalog のキー・ダウングレード)
- `REVIEW <file>:<line>:<text>` 行は、**許可リスト外に旧バージョンの文字列が残っている箇所**。1 行ずつ判断する:
  - このライブラリの版 (サンプルの build script、docs の埋め込みなど) → 手で直し、そのファイルを
    スクリプトの `allowlist()` に追加する (次回から自動で直る)
  - 別物 (他ライブラリの版がたまたま同じ値) → 何もしない

## Step 4. sanity check

```bash
./gradlew logVersion -q
# @bootup:if integration-test
./gradlew -p integrationTest help -q
# @bootup:end
```

新しいバージョンが表示され、設定フェーズが通ることを確認する。落ちたら commit せずユーザーに報告する。

## Step 5. 差分を確認して commit

```bash
git diff
```

意図したファイル・行だけが変わっていることを確認してから commit する:

```bash
git add gradle/libs.versions.toml <変更されたファイル>
git commit -m "chore(release): bump to <version>"
```

**push はしない。** 次の手順 (push → GitHub Release を published → publish.yml が Maven Central へ publish)
はユーザーが判断する。commit 後に「push して tag `v<version>` の GitHub Release を作ると
publish workflow が走る」と案内して止まる。リリースノートは `release-note` skill で作る。

## 触らないもの

- 他ライブラリのバージョン (Kotlin / kotest / Dokka 等)。これは catalog を直接編集する
- `api/*.api`、lockfile、`docs/public/api-docs/` (生成物)
- `.local/**`

## FAQ

- **目標が現在と同じ** → スクリプトが `OK already at` と出して何もしない
- **目標が現在より古い** → スクリプトが止める。Maven Central の版は取り消せないので、
  本当に必要なときだけユーザーの明示確認を取って `--allow-downgrade` を付ける
- **README が `<example-lib-version>` のままで書き換わらない** → 正常。README の Setup は
  バージョンをプレースホルダ + Maven Central バッジで示す方針で、リテラルの版を持たない
