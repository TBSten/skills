---
type: llm
focus: trace
weight: 1
---

ArticleListViewModel passes the loader to the ViewModel(...) constructor so it is closed with the ViewModel, calls initialLoad() in init, and calls refresh() for pull-to-refresh.
