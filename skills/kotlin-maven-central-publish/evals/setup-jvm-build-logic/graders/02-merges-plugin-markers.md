---
type: llm
focus: trace
weight: 1
---

build-logic/build.gradle.kts is edited so its dependencies include the Vanniktech maven publish plugin (e.g. via libs.plugins.mavenPublish as a plugin marker) alongside the Kotlin Gradle plugin, as the script's action_required asked.
