package com.acme.app.util

fun String.ellipsize(max: Int): String = if (length <= max) this else take(max - 1) + "…"
