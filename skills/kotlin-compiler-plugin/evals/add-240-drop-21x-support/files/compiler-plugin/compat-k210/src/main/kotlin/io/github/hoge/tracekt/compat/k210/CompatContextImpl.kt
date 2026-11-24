package io.github.hoge.tracekt.compat.k210

import io.github.hoge.tracekt.compat.CompatContext

class CompatContextImpl : CompatContext {
    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.1.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
