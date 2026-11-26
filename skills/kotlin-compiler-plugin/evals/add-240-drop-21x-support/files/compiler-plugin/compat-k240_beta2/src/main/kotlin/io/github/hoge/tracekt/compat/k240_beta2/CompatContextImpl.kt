package io.github.hoge.tracekt.compat.k240_beta2

import io.github.hoge.tracekt.compat.CompatContext

class CompatContextImpl : CompatContext by io.github.hoge.tracekt.compat.k230.CompatContextImpl() {
    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.0-Beta2"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
