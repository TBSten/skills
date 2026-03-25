---
paths:
  - "**/commonTest/**/*.kt"
  - "**/jvmSnapshotTest/**/*.kt"
  - "core/testing/**/*.kt"
  - "ui/core/testing/**/*.kt"
---

テストコードまたはテスト基盤のコードを変更する際は、必ず以下のテストドキュメントを読んでから作業すること。

- @docs/test/README.md
- 変更対象に対応するドキュメント:
  - **/commonTest/**/*.kt → @docs/test/unit-test.md
  - **/jvmSnapshotTest/**/*.kt → @docs/test/snapshot-test.md, @docs/test/compose-snapshot-test.md
  - core/testing/**/*.kt, ui/core/testing/**/*.kt → @docs/test/snapshot-test.md, @docs/test/compose-snapshot-test.md
