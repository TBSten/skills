package com.example.ksppluginsetup.ksp.core.common

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSName

/**
 * The project's single write-out point. Every generated file goes through here, which is what keeps
 * the `package` line and the import boilerplate in one place instead of duplicated per feature.
 *
 * Generation is transactional: [block] writes **declarations only** into an in-memory buffer first.
 * If it wrote nothing (every candidate was skipped), no file is opened at all — an empty
 * `package` + `import` file is worse than no file, because it still lands in the compilation.
 *
 * `Dependencies(aggregating = true, ...)` marks the output as depending on more than the containing
 * file, which is correct for any processor that looks across declarations.
 */
internal fun CodeGenerator.createNewKotlinFile(
    dependencies: Dependencies,
    packageName: KSName,
    fileName: String,
    block: (Appendable) -> Unit,
) {
    val buffer = StringBuilder()
    block(buffer)
    val body = buffer.toString()

    if (body.isEmpty()) return

    createNewFile(
        dependencies = dependencies,
        packageName = packageName.asString(),
        fileName = fileName,
    ).bufferedWriter()
        .use {
            it.appendLine("package ${packageName.asString()}")
            it.appendLine()
            it.appendLine("import com.example.ksppluginsetup.*")
            it.appendLine()
            it.append(body)
        }
}
