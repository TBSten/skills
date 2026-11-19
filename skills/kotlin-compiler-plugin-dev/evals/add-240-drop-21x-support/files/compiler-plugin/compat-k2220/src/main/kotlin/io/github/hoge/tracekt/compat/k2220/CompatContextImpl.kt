package io.github.hoge.tracekt.compat.k2220

import io.github.hoge.tracekt.compat.CompatContext

class CompatContextImpl : CompatContext by io.github.hoge.tracekt.compat.k210.CompatContextImpl() {
    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.2.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
