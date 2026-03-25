package com.example.snapshot.testing.snapshot.internal

import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal object SnapshotRegistry {
    private val usedFiles: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun markUsed(file: File) {
        usedFiles.add(file.canonicalPath)
    }

    fun getUsedFiles(): Set<String> = usedFiles.toSet()

    fun clear() {
        usedFiles.clear()
    }
}
