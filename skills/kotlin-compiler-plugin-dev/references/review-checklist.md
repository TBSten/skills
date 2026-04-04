# Compiler Plugin レビューチェックリスト

## K2 対応

- [ ] `CompilerPluginRegistrar` を使用しているか (K2 対応の前提条件)
- [ ] `ComponentRegistrar` (K1 のみ) を使っていないか
- [ ] `supportsK2 = true` が設定されているか
- [ ] FIR Extension を使っているか (K2 で FIR が必要な場合)
- [ ] K1 専用の Extension (`SyntheticResolveExtension`, `StorageComponentContainerContributor` 等) を使っていないか

## パターン 1: FIR 宣言生成 + IR 本体生成

- [ ] FIR で宣言 (シグネチャ) を生成し、IR で本体を生成する 2 段階になっているか
- [ ] FIR の `getCallableNamesForClass()` / `getNestedClassifiersNames()` で生成対象の名前を正しく返しているか
- [ ] FIR で生成した宣言の `GeneratedDeclarationKey` と IR での検索が一致しているか
- [ ] `FirAdditionalCheckersExtension` でアノテーション使用の妥当性をチェックしているか

## パターン 2: FIR チェッカー + IR 変換

- [ ] 変換対象の特定方法は適切か (アノテーション判定、型チェック等)
- [ ] `IrElementTransformerVoid` または `IrElementVisitorVoid` を適切に使い分けているか
- [ ] 変換順序に依存関係がある場合、正しい順序で実行しているか
- [ ] FIR チェッカーで不正な使用を事前にエラーとして報告しているか

## パターン 3: 複数 Lowering パス

- [ ] パス間の依存関係が正しく管理されているか
- [ ] 各パスの責務が明確に分離されているか
- [ ] パスの実行順序は正しいか

## コード品質

- [ ] `IrPluginContext` のみから情報を取得しているか (FIR の情報を IR で直接参照していないか)
- [ ] エラー報告は `FirAdditionalCheckersExtension` 経由で行っているか (IR フェーズでのエラー報告は避ける)
- [ ] テストが存在するか (kctfork / KotlinCompilation を使った unit test)
- [ ] マルチプラットフォーム対応が必要な場合、各プラットフォームでの動作を考慮しているか
- [ ] `GeneratedDeclarationKey` を使って生成した宣言を識別しているか (origin による判定)
