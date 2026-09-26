# リリースフローの選択肢

`example/publish.yml` / `example/publish-check.yml` (setup-publish.sh が生成) の設計理由と代替フロー。

## 既定: GitHub Release 駆動 (publish.yml)

1. SSoT (catalog `[versions]` or `gradle.properties` の `VERSION_NAME`) をリリース版に上げて main にマージ
2. GitHub で `v<版>` の tag を付けた Release を publish する (pre-release も可)
3. publish.yml が起動 → tag と SSoT の一致チェック → 非 SNAPSHOT チェック → `check` → `publishAndReleaseToMavenCentral`

| 設定 | 理由 |
|---|---|
| `release: types: [published]` | full release / pre-release の両方で発火する唯一の type。`prereleased` は draft から公開した pre-release で発火せず、`released` は pre-release で発火しない |
| `workflow_dispatch` の `dry_run` (既定 true) | Actions タブから誤って実行しても upload しない。`check` + `publishToMavenLocal` だけ走る |
| `workflow_dispatch` の `stage_only` | upload はするが Central Portal 上で手動 Publish するまで公開しない (`publishToMavenCentral`)。人の目で確認したい時用 |
| tag == バージョンのチェック | Release の tag と成果物の版のずれ (上げ忘れ) を upload 前に止める。`v` prefix は除去して比較 |
| 非 SNAPSHOT チェック | SNAPSHOT 版は vanniktech が snapshot リポジトリへ送ってしまうため |
| `concurrency: publish` + `cancel-in-progress: false` | 2 つの publish が Central に並走するのを防ぎつつ、走っている publish は止めない |
| `--no-configuration-cache` | publish は 1 回きりで CC の恩恵が無く、署名鍵などの秘密情報を CC エントリに残さないため。`gradle.properties` で CC を有効にしていても publish 時だけ無効化する |
| 失敗時の reports upload | CI 上の `check` 失敗を手元で再現せずに調べられる |
| runner | Apple (ios/macos/tvos/watchos) ターゲットがある時だけ `macos-latest` (Apple の klib は macOS でしかビルドできない)。無ければ安価な `ubuntu-latest`。script が `*.gradle.kts` から判定する |

`check` が重いプロジェクトは公開対象モジュールの check (`:lib:check` 等) に絞る。

## 代替: tag push 駆動 + GitHub Release 自動生成

Release を手で作らず、tag push を起点に publish と Release 作成までまとめて行う方式 (capture-compiler-plugin)。
Release ノートを commit log から自動生成したい時に向く。publish.yml の `on:` と末尾を次のように差し替える:

```yaml
on:
  push:
    tags: [ 'v*.*.*' ]
  workflow_dispatch:
    inputs:
      dry_run: { type: boolean, default: true }

jobs:
  publish:
    permissions:
      contents: write   # GitHub Release の作成に必要
    steps:
      - uses: actions/checkout@v7
        with:
          fetch-depth: 0   # release notes 生成に tag 履歴が要る
      # ... (バージョンチェック / check / publish は publish.yml と同じ。tag 一致チェックの条件は github.event_name == 'push' にする)
      - name: Create GitHub Release
        if: ${{ github.event_name == 'push' }}
        uses: softprops/action-gh-release@v3
        with:
          tag_name: ${{ github.ref_name }}
          generate_release_notes: true
          prerelease: ${{ contains(github.ref_name, '-') }}   # v1.0.0-rc1 等は pre-release
```

## PR での公開設定の検証 (publish-check.yml)

PR ごとに `publishToMavenLocal` を回し、POM・署名条件・javadoc/sources jar・artifactId の破損をリリース前に検出する (compose-preview-lab)。
署名は convention 側で `publishToMavenLocal` 時にスキップされるので Secrets 不要。既存 CI に job として統合したい場合は `--skip-publish-check` で生成を止め、同じ step を既存 workflow に足す。
