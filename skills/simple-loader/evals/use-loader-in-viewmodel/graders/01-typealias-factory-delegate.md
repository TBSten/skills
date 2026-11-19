---
type: llm
focus: trace
weight: 1
---

The loader is declared as a typealias of SimpleLoader<List<Article>> and implemented by delegating to SimpleLoaderFactory.create(...) (class delegation with by), with load calling ArticleRepository.fetchLatest().
