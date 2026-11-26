// =============================================================================
// compiler-plugin モジュール: src/main/kotlin/{package}/compiler/fir/
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}              - ユーザー指定のパッケージ名
//   {{COLLECTABLE_FQN}}      - @Collectable の FQN (例: com.example.collector.Collectable)
//   {{HINTS_PACKAGE}}        - hint 関数パッケージ (例: com.example.collector.hints)
//   {{HINT_PREFIX}}          - hint 関数名の prefix (例: collect)

// ---- FirExtensionRegistrar ----

package {{PACKAGE}}.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CollectorFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::HintFirGenerator
    }
}

// ---- HintFirGenerator ----

package {{PACKAGE}}.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIR フェーズで @Collectable 付きクラスを検出し、
 * hint パッケージに hint 関数スタブを生成する。
 *
 * 生成される hint 関数:
 *   package {{HINTS_PACKAGE}}
 *   fun collect_<tag>(collected: <TargetClass>): Unit
 */
class HintFirGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

    companion object {
        // @Collectable アノテーションの ClassId
        val COLLECTABLE_CLASS_ID = ClassId.topLevel(FqName("{{COLLECTABLE_FQN}}"))

        // hint 関数パッケージ
        val HINTS_PACKAGE = FqName("{{HINTS_PACKAGE}}")

        // hint 関数名の prefix
        const val HINT_PREFIX = "{{HINT_PREFIX}}"

        fun hintFunctionName(tag: String): Name {
            val effectiveTag = tag.ifEmpty { "default" }
            return Name.identifier("${HINT_PREFIX}_$effectiveTag")
        }
    }

    // @Collectable を検出する predicate
    private val predicate = LookupPredicate.create {
        annotated(COLLECTABLE_CLASS_ID.asSingleFqName())
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    /**
     * hint パッケージに生成する関数の CallableId を返す。
     *
     * FIR が @Collectable 付きクラスを発見するたびに呼ばれ、
     * 対応する hint 関数の ID を登録する。
     */
    override fun getTopLevelCallableIds(): Set<CallableId> {
        // predicate にマッチしたクラスを取得
        val matchedClasses = session.predicateBasedProvider
            .getSymbolsByPredicate(predicate)

        return matchedClasses.mapNotNull { symbol ->
            // @Collectable の tag 引数を取得
            val tag = extractTag(symbol) ?: ""
            CallableId(HINTS_PACKAGE, hintFunctionName(tag))
        }.toSet()
    }

    /**
     * hint 関数スタブを生成する。
     *
     * 各 @Collectable クラスに対して:
     *   fun collect_<tag>(collected: <TargetClass>): Unit
     */
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        if (callableId.packageName != HINTS_PACKAGE) return emptyList()

        val matchedClasses = session.predicateBasedProvider
            .getSymbolsByPredicate(predicate)

        return matchedClasses.mapNotNull { classSymbol ->
            val tag = extractTag(classSymbol) ?: ""
            val expectedName = hintFunctionName(tag)
            if (callableId.callableName != expectedName) return@mapNotNull null

            createTopLevelFunction(
                CollectorPluginKey,
                callableId,
                session.builtinTypes.unitType.coneType,
            ) {
                valueParameter(
                    Name.identifier("collected"),
                    classSymbol.defaultType(),
                )
            }.symbol
        }
    }

    /**
     * @Collectable(tag = "xxx") から tag を取得する。
     *
     * NOTE: 実際の実装では FirAnnotation からの引数抽出が必要。
     * session.annotationPlatformSupport や FirAnnotation.findArgumentByName() を使用。
     */
    private fun extractTag(symbol: Any): String? {
        // TODO: FirClassSymbol から @Collectable アノテーションの tag 引数を抽出
        // val annotation = symbol.resolvedAnnotations.find { it.classId == COLLECTABLE_CLASS_ID }
        // return annotation?.findArgumentByName(Name.identifier("tag"))?.extractStringValue()
        return ""
    }
}

/**
 * FIR 生成コードの識別キー。
 * IR フェーズで FIR 生成シンボルを識別するために使用。
 */
object CollectorPluginKey : org.jetbrains.kotlin.GeneratedDeclarationKey()
