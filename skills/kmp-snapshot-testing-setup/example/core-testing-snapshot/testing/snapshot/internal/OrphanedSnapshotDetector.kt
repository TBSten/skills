package com.example.snapshot.testing.snapshot.internal

import io.kotest.core.listeners.AfterProjectListener
import java.io.File

internal class OrphanedSnapshotDetector : AfterProjectListener {
    override suspend fun afterProject() {
        val flavor = System.getProperty("snapshot-test-flavor", "")
        if (flavor != "verify") return

        val outputDir = System.getProperty("snapshot-test-output-dir") ?: return
        val snapshotDir = File(outputDir)
        if (!snapshotDir.exists()) return

        val usedFiles = SnapshotRegistry.getUsedFiles()

        // 使用されたファイルが属する最上位ディレクトリのみをスキャン対象にする。
        // 複数モジュールが同一 snapshotDir を共有するため、他モジュールのファイルを
        // 孤立と誤判定しないようにスコープを限定する。
        val ownedTopDirs = usedFiles.mapNotNull { path ->
            val rel = File(path).relativeTo(snapshotDir).path
            rel.split(File.separator).firstOrNull()
        }.toSet()

        val orphanedFiles = ownedTopDirs
            .map { snapshotDir.resolve(it) }
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && !it.name.startsWith(".") }
                    .filter { !it.name.contains(".actual.") && !it.name.contains(".diff.") }
                    .filter { it.canonicalPath !in usedFiles }
                    .toList()
            }

        SnapshotRegistry.clear()

        if (orphanedFiles.isNotEmpty()) {
            val fileList = orphanedFiles.joinToString("\n") { "  - ${it.absolutePath}" }
            // REMOVE 検出のため .expected → .removed にリネーム（レポートで中身を読めるようにする）
            orphanedFiles.forEach { file ->
                val removedName = file.name.replace(".expected.", ".removed.")
                if (removedName != file.name) {
                    file.renameTo(File(file.parent, removedName))
                } else {
                    file.delete()
                }
            }
            error(
                "孤立したスナップショットファイルが ${orphanedFiles.size} 件見つかりました。\n" +
                    "テストが削除された場合は ./gradlew jvmSnapshotTestRecord を実行してください。\n" +
                    fileList,
            )
        }
    }
}
