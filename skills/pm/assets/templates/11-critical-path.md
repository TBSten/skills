---
pm_id: "11"
pm_phase: C
title: ネットワーク図・クリティカルパス
project: {{project}}
updated: {{date}}
---

# ネットワーク図・クリティカルパス

> 判断したいこと: 何が遅れるとまずい？

作業・所要日数・先行作業を下の表に書き、`pm.mjs calc` を実行するとクリティカルパスと図が下に書き込まれる。

<!-- pm:calc cpm -->
| ID | 作業 | 日数 | 先行作業 |
|---|---|---|---|
| A | TODO | 3 | |
| B | TODO | 2 | A |
| C | TODO | 4 | A |
| D | TODO | 1 | B, C |
<!-- /pm:calc -->

<!-- pm:result cpm -->
<!-- /pm:result -->

## 打ち手

- クリティカルパス上で短縮・支援できる作業: TODO
