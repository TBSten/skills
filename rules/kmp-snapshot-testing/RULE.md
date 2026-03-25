---
paths:
  - "**/jvmSnapshotTest/**/*.kt"
  - "core/testing/**/*.kt"
  - "ui/core/testing/**/*.kt"
---

スナップショットテストまたはテスト基盤のコードを変更する際は、必ず以下のテストドキュメントを読んでから作業すること。

- @docs/test/README.md
- @docs/test/snapshot-test.md
- @docs/test/compose-snapshot-test.md

## セットアップ

スナップショットテスト基盤がまだセットアップされていない場合
(convention-kmp-snapshot-testing plugin や core/testing/snapshot モジュールが存在しない場合) は、
`kmp-snapshot-testing-setup` スキルを使ってセットアップすること。

```sh
npx skills add tbsten/skills --skill kmp-snapshot-testing-setup
```
