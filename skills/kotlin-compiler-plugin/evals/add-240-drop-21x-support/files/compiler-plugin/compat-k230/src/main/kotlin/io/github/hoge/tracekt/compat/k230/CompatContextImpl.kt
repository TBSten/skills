package io.github.hoge.tracekt.compat.k230

import io.github.hoge.tracekt.compat.CompatContext

class CompatContextImpl : CompatContext by io.github.hoge.tracekt.compat.k2220.CompatContextImpl() {
    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.0"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
