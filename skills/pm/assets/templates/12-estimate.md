---
pm_id: "12"
pm_phase: C
title: 工数・コスト見積り表
project: {{project}}
updated: {{date}}
---

# 工数・コスト見積り表

> 判断したいこと: 予算と納期は現実的？

三点見積り（PERT: E = (楽観 + 4 × 最頻 + 悲観) / 6）。表を埋めて `pm.mjs calc` を実行すると見積りと合計が下に書き込まれる。

<!-- pm:calc pert -->
| 作業 | 楽観 | 最頻 | 悲観 |
|---|---|---|---|
| TODO | 3 | 5 | 7 |
<!-- /pm:calc -->

<!-- pm:result pert -->
<!-- /pm:result -->

## 前提・根拠

- 単位: TODO（人日 など）
- 見積りの前提: TODO
- 単価・コスト換算: TODO
- 予備（バッファ）の考え方: TODO: 悲観との差が大きい作業に厚く持つ
