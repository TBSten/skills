---
type: llm
focus: trace
weight: 1
---

The existing AppNavHost.kt NavDisplay is given a sceneStrategy chain with MainTabSceneStrategy first, and its Home, Search and Notifications entries get metadata = MainTabSceneStrategy.mainTab(...) while Settings and ArticleDetail do not.
