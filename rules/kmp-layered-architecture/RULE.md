---
paths:
  - "app/**/*.kt"
  - "ui/**/*.kt"
  - "domain/**/*.kt"
  - "data/**/*.kt"
---

App / UI / Domain / Data 層のコードを変更する際は、必ず以下のアーキテクチャドキュメントを読んでから作業すること。

- @docs/architecture/README.md
- 変更対象の層に対応するドキュメント:
  - アプリ全体 (app/**/*.kt) → @docs/architecture/app.md
  - UI・各画面のコード(ui/**/*.kt) → @docs/architecture/ui.md
  - UseCase/Domain モデルなどドメイン(domain/**/*.kt) → @docs/architecture/domain.md
  - API 通信・ローカル保存等のデータ操作 (data/**/*.kt) → @docs/architecture/data.md
