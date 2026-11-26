---
pm_id: "27"
pm_phase: F
title: 選択肢評価表
project: {{project}}
updated: {{date}}
---

# 選択肢評価表

> 判断したいこと: どの技術・仕様を選ぶ？

評価項目の重み（1〜5）と各選択肢の評価（1〜5）を書いて `pm.mjs calc` を実行すると、重み付き合計と順位が下に書き込まれる。1 行目は重み。

<!-- pm:calc matrix -->
| 選択肢 | TODO 項目1 | TODO 項目2 | TODO 項目3 |
|---|---|---|---|
| 重み | 5 | 4 | 3 |
| TODO 案A | 3 | 3 | 3 |
| TODO 案B | 3 | 3 | 3 |
<!-- /pm:calc -->

<!-- pm:result matrix -->
<!-- /pm:result -->

## 評価の根拠

- TODO: 評価値の理由（チームの技術力・既存コードなど）

## 結論

TODO: 採用案。決定は ADR に残す。
