---
type: llm
focus: trace
weight: 1
---

The assistant runs the skill's bundled scripts/install.sh with --package com.acme.shop.ui.core.loader and a --parts list that omits partial (e.g. core,logger,handler,ext,ui), instead of hand-writing the loader classes.
