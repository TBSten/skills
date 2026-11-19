---
name: compose-screenshot-update
description: >
  Compose の UI を変更したときに Roborazzi のスクリーンショットを記録・確認・検証し、
  PR に before / after を貼るまでを行う。
  Use when requested: "スクショテスト更新", "roborazzi record", "update screenshot tests".
metadata:
  status: Experimental
  group: テスト・検証
---

# compose-screenshot-update

1. `./gradlew recordRoborazziDebug` で差分を記録する
2. `build/outputs/roborazzi/` の差分画像を確認する
3. `./gradlew verifyRoborazziDebug` が通ることを確認する
4. PR 本文に before / after 画像を貼る
