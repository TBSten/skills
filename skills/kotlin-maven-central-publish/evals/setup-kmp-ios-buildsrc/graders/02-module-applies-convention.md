---
type: llm
focus: trace
weight: 1
---

kvstore-core/build.gradle.kts ends up applying id("publish-convention") with group = "dev.hoge.kvstore" and a version read from libs.versions.kvstore, and does not call coordinates(...) itself.
