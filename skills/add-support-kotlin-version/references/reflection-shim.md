# Reflection-Based Shim (新 compat module を作らずに小さな差分を吸収)

## いつ使う

Kotlin compiler API は patch release でも稀に **bytecode-level の API shape が変わる** ことがある:

- `IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA` の dispatch shape: 2.3.0/10 では `IrDeclarationOrigin.Companion.getLOCAL_FUNCTION_FOR_LAMBDA()` (getter)、 2.3.20+ では static field GET に変わった
- `IrUtilsKt.getAnnotation(...)` の return type: Kotlin 2.4 で `IrConstructorCall?` → `IrAnnotation?` に narrow された
- enum entry の名前は同じだが内部 representation が変わって直接 import 不可

これらは「**呼ぶ側のソースコード上の見た目は同じ**」 だが、 baseline Kotlin で compile したバイトコードを別 Kotlin で動かすと `NoSuchMethodError` / `IncompatibleClassChangeError` になる。

このとき **新 compat module を作るのは過剰** — 影響範囲が 1 シンボルだけなので、 `compat/` の **shared SPI 側に reflection-based 解決ロジック** を置く方が綺麗。

## ガイドライン

1. **影響範囲が 1 シンボル / 1 enum entry / 1 method** に閉じるなら reflection shim
2. **影響範囲が複数メソッド・extension 全体** なら新 compat module
3. **どちらかというと迷ったら reflection shim** から始める。 後から compat module に昇格させるのは容易だが、 compat module を作ってしまうと delegation chain の更新が連鎖する

## 例: `IrDeclarationOriginCompat`

```kotlin
package me.tbsten.compose.preview.lab.compiler.compat

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

/**
 * Resolves [IrDeclarationOrigin] enum entries across Kotlin patch versions.
 *
 * Why reflection: the dispatch shape of `IrDeclarationOrigin.<NAME>` differs across
 * 2.3 patches:
 * - 2.3.0 / 2.3.10: `Companion.getXxx()` (getter on the Companion object)
 * - 2.3.20+:        static field GET on the class itself
 *
 * Both forms exist for the same identifier from a Kotlin source perspective, but the
 * generated JVM bytecode differs. Compiling against one baseline and running on the
 * other surfaces `NoSuchMethodError`. Reflection lets us absorb the diff in one place
 * without spinning up a new compat module for a single enum entry shift.
 */
object IrDeclarationOriginCompat {
    val LOCAL_FUNCTION_FOR_LAMBDA: IrDeclarationOrigin by lazy {
        resolveOrigin("LOCAL_FUNCTION_FOR_LAMBDA")
    }

    val DELEGATE: IrDeclarationOrigin by lazy {
        resolveOrigin("DELEGATE")
    }

    private fun resolveOrigin(name: String): IrDeclarationOrigin {
        val clazz = Class.forName("org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin")

        // 2.3.20+: static field GET (DELEGATE は IrDeclarationOriginImpl 側にあるケースもあるので両方試す)
        runCatching {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            return field.get(null) as IrDeclarationOrigin
        }

        // 2.3.0 / 2.3.10: Companion.getXxx()
        runCatching {
            val companionField = clazz.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companion = companionField.get(null)
            val getter = companion.javaClass.getMethod("get" + name.toCamelCase())
            return getter.invoke(companion) as IrDeclarationOrigin
        }

        // 2.4 以降で IrDeclarationOriginImpl に移ったケースの fallback
        runCatching {
            val implClazz = Class.forName("org.jetbrains.kotlin.ir.declarations.impl.IrDeclarationOriginImpl")
            val field = implClazz.getDeclaredField(name)
            field.isAccessible = true
            return field.get(null) as IrDeclarationOrigin
        }

        error("Cannot resolve IrDeclarationOrigin.$name on this Kotlin version. " +
            "Please file a bug report.")
    }

    private fun String.toCamelCase(): String =
        split("_").joinToString("") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
}
```

call site:

```kotlin
// Before (baseline 2.3.x の bytecode が 2.3.0 / 2.4 で死ぬ):
if (declaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) { ... }

// After (全 2.3.x + 2.4 で動く):
if (declaration.origin == IrDeclarationOriginCompat.LOCAL_FUNCTION_FOR_LAMBDA) { ... }
```

## 例: `IrAnnotationCompat`

Kotlin 2.4 で `IrUtilsKt.getAnnotation(...)` の return type が変わった (`IrConstructorCall?` → `IrAnnotation?` で型階層が広がった)。 baseline 2.3 でコンパイルしたコードを 2.4 で動かすと `IncompatibleClassChangeError`。

```kotlin
object IrAnnotationCompat {
    /**
     * Walks [IrAnnotationContainer.annotations] directly to find an annotation by class FQN.
     *
     * Replaces direct usage of `IrUtilsKt.getAnnotation(FqName)` which has a different
     * bytecode signature between Kotlin 2.3 and 2.4 due to the `IrAnnotation` type
     * widening in 2.4.
     */
    fun getAnnotation(container: IrAnnotationContainer, fqName: FqName): IrConstructorCall? {
        return container.annotations
            .firstOrNull { call ->
                // ここも `call.symbol.owner` の return type は変わらないので安全
                val symbol = (call as? IrConstructorCall)?.symbol ?: return@firstOrNull false
                symbol.owner.parentAsClass.fqNameWhenAvailable == fqName
            } as? IrConstructorCall
    }
}
```

## 命名規則

- 1 file = 1 shim object — まとめると粒度が大きくなりすぎる
- ファイル名: `IrXxxCompat.kt` / `FirXxxCompat.kt` の形で **原 API のクラス名 + Compat**
- shim object 内では `runCatching` で **失敗予想されるパターンから順に試す** → 全部失敗したら `error("...")`
- KDoc に **どの Kotlin バージョンで何が変わったか / KT-xxxxx の link** を書く (= 「future-you が見て shim を消せるか判断できる」 状態)

## チェックリスト

- [ ] shim を追加した時、 `compat/` ディレクトリ直下に置く (compat-kXX 各 module ではない)
- [ ] KDoc の冒頭で **何が変わったか / どの Kotlin patch で変化したか** を 1 段落で書く
- [ ] `runCatching` で複数 candidate を順に試す。 全失敗時は **明示的な error message** を投げる (silent fallback はしない)
- [ ] テストで 2 つ以上のバージョンに対して shim が動くことを確認 (kctfork で 2.3.0 と 2.3.20 両方で test 走らせる)
- [ ] `docs/support-kotlin-versions.md` の "Binary incompatibilities absorbed via reflection" セクションにエントリを追加 (= 「あとでこの shim を消せるかどうか」 を一覧化)
- [ ] 影響範囲が広がってきたら早めに新 compat module に昇格 (1 shim object で 5+ symbol を扱うようになったら検討)
