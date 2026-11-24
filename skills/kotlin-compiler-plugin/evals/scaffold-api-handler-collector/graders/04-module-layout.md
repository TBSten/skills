---
type: llm
focus: trace
weight: 1
---

settings.gradle.kts includes separate annotations, compiler-plugin and gradle-plugin modules, with the annotations module using Kotlin Multiplatform (commonMain) and the compiler-plugin module depending on kotlin-compiler-embeddable as compileOnly.
