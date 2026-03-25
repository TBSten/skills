# App Layers

```text
+---------------+
|               ↓
Domain ← UI ← App
↑             ↑
Data ---------+
```

## Basic Principles

- テスト容易性を高めるために interface と実装に分割する際の命名規則:
    - interface はそのままの名前
    - 本実装は interface 名 + `Impl`
    - Fake は `Fake` + interface 名
- Layer 間の依存関係は事前にこのドキュメントで定義されたものから外れないようにする。Layer 内に閉じた依存関係は特に制限しない。

## Layers

| Layer                         | 対象モジュール, ディレクトリ   |
|-------------------------------|--------------------------|
| [Layer1. App](./app.md)       | app モジュール              |
| [Layer2. UI](./ui.md)         | ui/** モジュール            |
| [Layer3. Domain](./domain.md) | domain/** モジュール        |
| [Layer4. Data](./data.md)     | data/** モジュール          |

## Exception. Core

- どのモジュールからも参照されうる言語拡張的な機能を含む。
- このモジュールには一切のプロジェクト固有なロジック・情報・モデルを含まないようにする。
