// 配置先: jvmTest (全モジュールのソースを走査できるテストモジュール推奨)
// TODO: package をプロジェクトに合わせて置換する
package com.example.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FreeSpec

/**
 * docs/architecture/ の規約を Konsist で機械検証するテストテンプレート。
 *
 * 検証内容:
 * 1. 層間依存 — docs/architecture/README.md の依存図どおりか
 *    (UI -> Domain / Data -> Domain / App -> UI, Domain, Data のみ許可)
 * 2. DI Providers 命名 — `Providers` サフィックス強制、`Module` / 単数 `Provider` 禁止
 * 3. `Impl` / `Fake` 命名 — interface と実装の命名規則
 *
 * 依存: Konsist (`com.lemonappdev:konsist`) + Kotest FreeSpec。
 * TODO: Kotest を使わないプロジェクトでは JUnit の @Test メソッドに書き換える。
 *
 * 注意: Konsist は空の宣言リストへの assert を KoPreconditionFailedException で失敗させる。
 * 該当する宣言がまだ無いテスト (例: Fake クラスが 1 つも無い) は一時的にコメントアウトし、
 * 宣言を追加したタイミングで有効化する。
 */

// ---------------------------------------------------------------- TODO: 差し替え定数

/** TODO: プロジェクトのルートパッケージに置換する。 */
private const val PROJECT_ROOT = "com.example"

// TODO: 各層のパッケージ接頭辞をプロジェクトに合わせる。
//  パッケージが層を表していない場合は inLayer() を file.path ベース
//  (例: file.path.contains("/ui/")) の判定に書き換える。
private const val APP_PACKAGE = "$PROJECT_ROOT.app"
private const val UI_PACKAGE = "$PROJECT_ROOT.ui"
private const val DOMAIN_PACKAGE = "$PROJECT_ROOT.domain"
private const val DATA_PACKAGE = "$PROJECT_ROOT.data"

/** TODO: DI Providers 宣言に付くアノテーション名。Metro 以外の DI では変更する。 */
private const val DI_PROVIDERS_ANNOTATION = "ContributesTo"

// ---------------------------------------------------------------- ヘルパー

/** プロジェクト全体のソース (テストソース含む)。モジュールを絞る場合は scopeFromModule を使う。 */
private val allFiles: List<KoFileDeclaration> by lazy {
    Konsist.scopeFromProject().files
}

/** このファイルの package が [layerPackage] またはそのサブパッケージのとき true。 */
private fun KoFileDeclaration.inLayer(layerPackage: String): Boolean {
    val packageName = packagee?.name ?: return false
    return packageName == layerPackage || packageName.startsWith("$layerPackage.")
}

/** [importPrefixes] のいずれかで始まる FQN を import しているとき true。 */
private fun KoFileDeclaration.importsFrom(vararg importPrefixes: String): Boolean =
    imports.any { import -> importPrefixes.any { prefix -> import.name.startsWith(prefix) } }

// ---------------------------------------------------------------- テスト本体

internal class ArchitectureConventionTest :
    FreeSpec({
        // docs/architecture/README.md の依存図:
        //   Domain <- UI <- App / Domain <- Data / Data <- App
        //   (App は全層に依存できるが、どの層も App に依存しない)
        "層間依存 (docs/architecture/README.md)" - {
            "Domain 層は UI / Data / App に依存しない" {
                allFiles
                    .filter { it.inLayer(DOMAIN_PACKAGE) }
                    .assertFalse { file -> file.importsFrom("$UI_PACKAGE.", "$DATA_PACKAGE.", "$APP_PACKAGE.") }
            }

            "UI 層は Data / App に依存しない (Domain のみ依存可)" {
                allFiles
                    .filter { it.inLayer(UI_PACKAGE) }
                    .assertFalse { file -> file.importsFrom("$DATA_PACKAGE.", "$APP_PACKAGE.") }
            }

            "Data 層は UI / App に依存しない (Domain のみ依存可)" {
                allFiles
                    .filter { it.inLayer(DATA_PACKAGE) }
                    .assertFalse { file -> file.importsFrom("$UI_PACKAGE.", "$APP_PACKAGE.") }
            }
        }

        "DI Providers 命名 (docs/architecture/app.md)" - {
            "DI 宣言 (@$DI_PROVIDERS_ANNOTATION) の interface は Providers サフィックスに統一する" {
                allFiles
                    .flatMap { it.interfaces() }
                    .filter { declaration -> declaration.annotations.any { it.name == DI_PROVIDERS_ANNOTATION } }
                    .assertTrue { it.name.endsWith("Providers") }
            }

            "Module / 単数 Provider サフィックスの interface を作らない" {
                // 例外を許したい interface があればここで filter で除外し、理由をコメントに残す
                allFiles
                    .flatMap { it.interfaces() }
                    .assertFalse { it.name.endsWith("Module") || it.name.endsWith("Provider") }
            }
        }

        "Impl / Fake 命名 (docs/architecture/README.md)" - {
            "Impl クラスはサフィックスを除いた名前の interface を実装する" {
                allFiles
                    .flatMap { it.classes() }
                    .filter { it.name.endsWith("Impl") }
                    .assertTrue { cls -> cls.parents().any { it.name == cls.name.removeSuffix("Impl") } }
            }

            "Fake クラスはプレフィックスを除いた名前の interface を実装する" {
                allFiles
                    .flatMap { it.classes() }
                    .filter { it.name.startsWith("Fake") }
                    .assertTrue { cls -> cls.parents().any { it.name == cls.name.removePrefix("Fake") } }
            }
        }
    })
