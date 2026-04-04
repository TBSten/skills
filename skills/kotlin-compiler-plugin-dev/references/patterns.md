# Compiler Plugin 設計パターン

## パターン 1: FIR 宣言生成 + IR 本体生成 (最も一般的)

```
FirDeclarationGenerationExtension → 合成メンバの「宣言」を生成 (シグネチャのみ)
        ↓
IrGenerationExtension → 宣言の「本体」(IR) を生成
```

**採用**: serialization, parcelize, noarg, metro, koin, back-in-time, js-plain-objects, kotlin-dataframe

**いつ使うか**: 新しいメンバ (関数、プロパティ、コンストラクタ) やクラスを追加する場合

**実装のポイント**:
- FIR の `getCallableNamesForClass()` / `getNestedClassifiersNames()` で生成対象の名前を返す
- FIR で `GeneratedDeclarationKey` を設定し、IR で同じキーで検索する
- `FirAdditionalCheckersExtension` でアノテーション使用の妥当性をチェック

**最小の前例**: noarg (コンストラクタ1つの追加のみ)
**最も参考になる前例**: serialization (複数メンバの生成 + チェッカー + メタデータ)

## パターン 2: FIR チェッカー + IR 変換

```
FirAdditionalCheckersExtension → アノテーション使用の妥当性チェック
        ↓
IrGenerationExtension → 既存メソッドの変換 (toString 書き換え、ログ挿入等)
```

**採用**: redacted, kondition, suspend-kontext, aspectk, power-assert

**いつ使うか**: 既存のコードを変換・拡張する場合 (新しい宣言は追加しない)

**実装のポイント**:
- FIR チェッカーで不正な使用を事前にエラーとして報告
- IR では `IrElementTransformerVoid` で対象要素を訪問・変換
- 変換順序に依存関係がある場合は注意

**最小の前例**: redacted (toString メソッドの書き換えのみ)
**最も参考になる前例**: kondition (バリデーションコード挿入 + 値フィッティング)

## パターン 3: FIR のみ (IR 不要)

```
FirStatusTransformerExtension / FirAssignExpressionAltererExtension / FirDeclarationGenerationExtension
→ フロントエンドで完結する変更
```

**採用**: allopen, sam-with-receiver, assign-plugin, arrow-optics

**いつ使うか**: 
- クラス/メンバの修飾子を変更する場合 (open 化等)
- 代入式や SAM 変換を変更する場合
- companion object のみを追加する場合 (本体が不要)

**最小の前例**: allopen (FirStatusTransformerExtension のみ)

## パターン 4: IR のみ

```
IrGenerationExtension → アノテーション付き要素の IR を直接変換
```

**採用**: MoshiX, Zipline, DebugLog (K1)

**いつ使うか**: FIR フェーズでの処理が不要で、IR レベルの変換だけで済む場合

**注意**: FIR チェッカーがないため、不正な使用に対するエラー報告が IR フェーズになる (ユーザー体験が劣る)

## K1/K2 デュアルサポートの対応表

| K2 Extension | K1 対応 |
|---|---|
| `FirDeclarationGenerationExtension` | `SyntheticResolveExtension` |
| `FirAdditionalCheckersExtension` | `StorageComponentContainerContributor` → `DeclarationChecker` |
| `FirStatusTransformerExtension` | `DeclarationAttributeAltererExtension` |
| `FirMetadataSerializerPlugin` | `DescriptorSerializerPlugin` |
| `IrGenerationExtension` | `IrGenerationExtension` (共通) |

> **注**: 新規プラグインは K2 のみ対応で十分。K1 は非推奨化が進んでいる。

## FIR Extension Point 選択ガイド

| やりたいこと | 適切な Extension Point | 前例 |
|---|---|---|
| 新しいメンバ・クラスを合成生成 | `FirDeclarationGenerationExtension` | serialization, parcelize, noarg, lombok, metro, koin, arrow-optics, back-in-time, js-plain-objects, kotlin-dataframe |
| スーパータイプ (インターフェース) の追加 | `FirSupertypeGenerationExtension` | serialization, metro, back-in-time, kotlin-dataframe |
| クラス/メンバのステータスを変更 (open 化等) | `FirStatusTransformerExtension` | allopen, lombok, metro |
| バリデーション・診断エラーの報告 | `FirAdditionalCheckersExtension` | ほぼ全プラグイン (16+) |
| SAM 変換のカスタマイズ | `FirSamConversionTransformerExtension` | sam-with-receiver, scripting |
| カスタム関数型の登録 | `FirFunctionTypeKindExtension` | compose |
| メタデータへのカスタム情報保存 | `FirMetadataSerializerPlugin` | serialization |
| セッション固有のサービス/状態管理 | `FirExtensionSessionComponent` | redacted, metro, lombok, js-plain-objects, assign-plugin, compose, scripting |
| 代入式の変換 | `FirAssignExpressionAltererExtension` | assign-plugin |
| 関数呼び出しの戻り型精製 | `FirFunctionCallRefinementExtension` | kotlin-dataframe |
| 暗黙的レシーバ注入 | `FirExpressionResolutionExtension` | kotlin-dataframe |

## IR Extension Point 選択ガイド

| やりたいこと | 適切なアプローチ | 前例 |
|---|---|---|
| FIR で生成した宣言の本体を IR で生成 | `IrGenerationExtension` + `IrElementTransformerVoid` | serialization, parcelize, noarg, metro, koin, back-in-time |
| 既存メソッドの書き換え (toString 等) | `IrGenerationExtension` + `IrElementTransformerVoid` | redacted, kondition, aspectk |
| 関数呼び出しの前後にコードを挿入 | `IrGenerationExtension` + `IrElementTransformerVoid` | debuglog, power-assert, suspend-kontext |
| プラットフォーム別の変換 | 抽象 Transformer + プラットフォーム別実装 | atomicfu (JVM/JS/Native), compose (Native/JS 固有 Lowering) |
