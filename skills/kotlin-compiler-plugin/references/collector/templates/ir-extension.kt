// =============================================================================
// compiler-plugin モジュール: src/main/kotlin/{package}/compiler/ir/
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}           - ユーザー指定のパッケージ名
//   {{COLLECTABLE_FQN}}   - @Collectable の FQN
//   {{COLLECT_ALL_FQN}}   - @CollectAll の FQN
//   {{HINTS_PACKAGE}}     - hint 関数パッケージ
//   {{HINT_PREFIX}}       - hint 関数名の prefix

// ---- IrExtension (オーケストレーション) ----

package {{PACKAGE}}.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * IR フェーズのエントリポイント。3 つのフェーズを順に実行する。
 *
 * Phase 1: hint 関数に body を追加し、registerFunctionAsMetadataVisible() で公開
 * Phase 2: referenceFunctions() で hint 関数を検索し、収集対象クラスを復元
 * Phase 3: @CollectAll 関数の body を生成
 */
class CollectorIrExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Phase 1: hint 関数 body 生成 + metadata 登録
        val hintBodyGenerator = HintBodyGenerator(pluginContext, moduleFragment)
        hintBodyGenerator.generate()

        // Phase 2: cross-module discovery
        val discovery = Discovery(pluginContext)
        // tag ごとの収集結果は Phase 3 で使用

        // Phase 3: @CollectAll の body 生成
        val transformer = CollectAllTransformer(pluginContext, discovery)
        moduleFragment.transform(transformer, null)
    }
}

// ---- HintBodyGenerator (Phase 1) ----

package {{PACKAGE}}.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.io.path.Path

/**
 * FIR で生成された hint 関数スタブに body を追加し、
 * registerFunctionAsMetadataVisible() で downstream モジュールに公開する。
 *
 * IR フェーズで @Collectable クラスを走査し、各クラスに対して hint 関数を生成。
 * 生成される関数:
 *   package {{HINTS_PACKAGE}}
 *   fun collect_<tag>(collected: <TargetClass>): Unit = error("stub")
 */
class HintBodyGenerator(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment,
) {
    companion object {
        val COLLECTABLE_CLASS_ID = ClassId.topLevel(FqName("{{COLLECTABLE_FQN}}"))
        val HINTS_PACKAGE = FqName("{{HINTS_PACKAGE}}")
        const val HINT_PREFIX = "{{HINT_PREFIX}}"
    }

    fun generate() {
        // モジュール内の全クラスを走査し、@Collectable 付きクラスを収集
        val collectableClasses = mutableListOf<Pair<IrClass, String>>() // (class, tag)

        for (file in moduleFragment.files) {
            for (declaration in file.declarations) {
                if (declaration is IrClass &&
                    declaration.hasAnnotation(COLLECTABLE_CLASS_ID.asSingleFqName())
                ) {
                    val tag = extractTagFromIr(declaration)
                    collectableClasses.add(declaration to tag)
                }
            }
        }

        // 各 @Collectable クラスに対して hint 関数を生成
        for ((irClass, tag) in collectableClasses) {
            generateHintFunction(irClass, tag)
        }
    }

    private fun generateHintFunction(sourceClass: IrClass, tag: String) {
        val effectiveTag = tag.ifEmpty { "default" }
        val hintName = Name.identifier("${HINT_PREFIX}_$effectiveTag")

        // 1. IR 関数を構築
        val function = pluginContext.irFactory.buildFun {
            name = hintName
            returnType = pluginContext.irBuiltIns.unitType
        }.apply {
            parameters += buildValueParameter(this) {
                name = Name.identifier("collected")
                type = sourceClass.defaultType
                kind = IrParameterKind.Regular
            }
            // body は空 (hint 関数は呼び出されない)
            body = pluginContext.irFactory.createBlockBody(
                startOffset = -1,
                endOffset = -1,
            )
        }

        // 2. 合成 IrFile を作成
        val sourceFileName = sourceClass.fileEntry.name
        val hintFileName = "${sourceClass.name}_${effectiveTag}_hint.kt"
        val fakeNewPath = Path(sourceFileName).parent?.resolve(hintFileName)
            ?: Path(hintFileName)

        val firFile = buildFile {
            val metadataSource = sourceClass.metadata as? FirMetadataSource.Class
            if (metadataSource != null) {
                moduleData = metadataSource.fir.moduleData
            }
            origin = FirDeclarationOrigin.Synthetic.PluginFile
            packageDirective = buildPackageDirective {
                packageFqName = HINTS_PACKAGE
            }
            name = hintFileName
        }

        val hintFile = IrFileImpl(
            fileEntry = NaiveSourceBasedFileEntryImpl(fakeNewPath.toString()),
            packageFragmentDescriptor = org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor(
                moduleFragment.descriptor,
                HINTS_PACKAGE,
            ),
            module = moduleFragment,
        ).also { it.metadata = FirMetadataSource.File(firFile) }

        // 3. モジュールに追加し、metadata として登録
        moduleFragment.addFile(hintFile)
        hintFile.addChild(function)

        // 4. downstream モジュールに公開 (これが cross-module discovery の鍵)
        pluginContext.metadataDeclarationRegistrar
            .registerFunctionAsMetadataVisible(function)
    }

    private fun extractTagFromIr(irClass: IrClass): String {
        // TODO: IrClass の @Collectable アノテーションから tag 引数を取得
        // val annotation = irClass.annotations.find { ... }
        // return annotation.getValueArgument(0)?.let { (it as IrConst).value as String } ?: ""
        return ""
    }
}

// ---- Discovery (Phase 2) ----

package {{PACKAGE}}.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * hint パッケージから hint 関数を検索し、
 * パラメータ型から収集対象クラスを復元する。
 *
 * referenceFunctions(CallableId) が cross-module discovery の核心 API。
 * この API は downstream モジュールのコンパイル時に、
 * 依存 JAR 内の registerFunctionAsMetadataVisible() で登録された関数を返す。
 */
class Discovery(private val pluginContext: IrPluginContext) {

    companion object {
        val HINTS_PACKAGE = FqName("{{HINTS_PACKAGE}}")
        const val HINT_PREFIX = "{{HINT_PREFIX}}"
    }

    /**
     * 指定 tag の @Collectable クラスを全モジュールから収集する。
     *
     * @param tag 収集対象の tag ("" は "default" として扱う)
     * @return 収集された IrClass のリスト
     */
    fun discoverClasses(tag: String): List<IrClass> {
        val effectiveTag = tag.ifEmpty { "default" }
        val hintFunctionName = Name.identifier("${HINT_PREFIX}_$effectiveTag")
        val callableId = CallableId(HINTS_PACKAGE, hintFunctionName)

        // hint パッケージ内の関数を検索
        // ローカルモジュール + 依存 JAR の両方から見つかる
        val hintFunctions: Collection<IrSimpleFunctionSymbol> =
            pluginContext.referenceFunctions(callableId)

        // 各 hint 関数のパラメータ型から収集対象クラスを復元
        return hintFunctions.mapNotNull { functionSymbol ->
            val function = functionSymbol.owner
            val collectedParam = function.valueParameters.firstOrNull() ?: return@mapNotNull null
            val classSymbol = collectedParam.type.classOrNull ?: return@mapNotNull null
            classSymbol.owner
        }
    }
}

// ---- CollectAllTransformer (Phase 3) ----

package {{PACKAGE}}.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * @CollectAll 付き関数を検出し、body を生成する。
 *
 * 変換前:
 *   @CollectAll(tag = "handler")
 *   fun allHandlers(): List<KClass<*>>
 *
 * 変換後:
 *   fun allHandlers(): List<KClass<*>> = listOf(HandlerA::class, HandlerB::class)
 */
class CollectAllTransformer(
    private val pluginContext: IrPluginContext,
    private val discovery: Discovery,
) : IrElementTransformerVoid() {

    companion object {
        val COLLECT_ALL_CLASS_ID = ClassId.topLevel(FqName("{{COLLECT_ALL_FQN}}"))
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.hasAnnotation(COLLECT_ALL_CLASS_ID.asSingleFqName())) {
            return super.visitSimpleFunction(declaration)
        }

        val tag = extractTagFromCollectAll(declaration)
        val collectedClasses = discovery.discoverClasses(tag)

        // listOf(A::class, B::class, ...) を body として生成
        declaration.body = generateListOfKClassBody(declaration, collectedClasses)

        return declaration
    }

    /**
     * listOf(A::class, B::class, ...) を生成する。
     *
     * NOTE: 実際の実装では IrBuilderWithScope を使い、
     * irCall(listOfFunction) + irVararg(kClassReferences) を構築する。
     */
    private fun generateListOfKClassBody(
        function: IrFunction,
        classes: List<org.jetbrains.kotlin.ir.declarations.IrClass>,
    ): org.jetbrains.kotlin.ir.expressions.IrBody {
        // TODO: IrBuilder で以下を構築
        // val listOfFun = pluginContext.referenceFunctions(
        //     CallableId(FqName("kotlin.collections"), Name.identifier("listOf"))
        // ).single { it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].isVararg }
        //
        // irBlockBody {
        //   +irReturn(
        //     irCall(listOfFun).apply {
        //       putTypeArgument(0, kClassStarType)
        //       putValueArgument(0, irVararg(kClassStarType, classes.map { irClassReference(it) }))
        //     }
        //   )
        // }

        return pluginContext.irFactory.createBlockBody(-1, -1)
    }

    private fun extractTagFromCollectAll(function: IrSimpleFunction): String {
        // TODO: @CollectAll アノテーションから tag 引数を取得
        return ""
    }
}
