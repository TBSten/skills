# サポート Kotlin バージョン追加スキル

[English](./add-support-kotlin-version.md)

Kotlin Compiler Plugin プロジェクトのサポート対象 Kotlin バージョンを追加・削除する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills \
  --skill add-support-kotlin-version
```

### 2. AI エージェントに依頼:

```
このコンパイラプラグインプロジェクトに Kotlin 2.4.0 のサポートを追加して。
```

## このスキルでできること

複数バージョン対応基盤がすでにあるコンパイラプラグインプロジェクトに対し:

1. 対象 Kotlin バージョンが既存 compat module の範囲に収まるか、新 module が必要かを判定
2. CI matrix、kctfork バージョンマップ、version catalog、README を更新
3. 必要に応じて新 compat module を作成（既存コピー → パッケージリネーム → `minVersion` 更新）
4. 全サポートバージョンでテストを実行して動作確認

## 対応アーキテクチャ

### A: Compat Module Layer (metro スタイル)

`compiler-plugin/compat-kXX/` 形式のモジュールを持つプロジェクト。ServiceLoader がランタイムに最適な実装を選択する。

各 compat module は `minVersion` を持ち、`minVersion ≤ 現在の Kotlin バージョン` の中から最大のものが選ばれる。

### B: Source Set Separation (ソースセット分離)

`src/v2_0_0/kotlin/` や `src/pre_2_0_0/kotlin/` 形式のディレクトリを持つプロジェクト。Gradle がビルド時に Kotlin バージョンに応じてソースディレクトリを切り替える。

K1 (PSI/ComponentRegistrar) と K2 (FIR/CompilerPluginRegistrar) の大きな断絶を吸収するのに最適。

## 主要コンセプト

### compat module の minVersion 範囲

```
compat-k2000: minVersion="2.0.0"  → 2.0.0–2.0.1x をカバー
compat-k2020: minVersion="2.0.20" → 2.0.20–2.1.x をカバー
compat-k23:   minVersion="2.2.0"  → 2.2.0–(最新) をカバー
```

新 compat module が必要になるのは:
- 対象バージョンが既存 module の範囲外 かつ
- `NoSuchMethodError` / `NoClassDefFoundError` が発生する（API 境界）

### kctfork バージョンマップ

unit test は [kctfork (kotlin-compile-testing)](https://github.com/ZacSweers/kotlin-compile-testing) を使用。各 Kotlin バージョンに対応した kctfork が必要。新 major/minor 追加時に更新する。

### fail-fast: false の重要性

GitHub Actions の matrix で `fail-fast: false` を設定すると、実験的 / RC バージョンが失敗しても安定版のテスト結果が隠れずに確認できる。

## 前提条件

このスキルは複数バージョン対応基盤が既にあるプロジェクト向け。初期セットアップは `kotlin-compiler-plugin-setup` スキルの Step 10 を参照。

## 参考リンク

- kctfork リリース: https://github.com/ZacSweers/kotlin-compile-testing/releases
- Compose Multiplatform リリース: https://github.com/JetBrains/compose-multiplatform/releases
- Kotlin リリース: https://github.com/JetBrains/kotlin/releases
- 実装例: [ZacSweers/metro](https://github.com/ZacSweers/metro)、[kitakkun/multi-kotlin-support-example](https://github.com/kitakkun/multi-kotlin-support-example)
