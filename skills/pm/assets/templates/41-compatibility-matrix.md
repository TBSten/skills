---
pm_id: "41"
pm_phase: I
title: 移行・互換性マトリクス
project: {{project}}
updated: {{date}}
---

# 移行・互換性マトリクス

> 判断したいこと: どの順で出せば壊れない？

| クライアント \ サーバー | TODO: API v1 | TODO: API v2 |
|---|---|---|
| TODO: App v1.0 | 互換あり | TODO |
| TODO: App v2.0 | TODO | 互換あり |

## リリース順序

```mermaid
flowchart LR
  A["TODO: API v2 公開"] --> B["TODO: App v2 公開"]
  B --> C["TODO: 旧 API 廃止判断（旧バージョン利用率 N% 未満）"]
```

- 強制アップデートの条件: TODO
