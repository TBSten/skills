// Consumer-style usage: exactly what a user of the published plugin writes.
plugins {
    kotlin("jvm")
    id("com.example.compilerpluginsetup")
    application
}

application {
    mainClass = "sample.MainKt"
}

// Writing the DSL here also verifies the extension is registered (the script fails to compile otherwise).
examplePlugin {
    enabled = true
}

// Prints what the Gradle plugin wired, so the E2E test can assert on it.
tasks.register("printE2eWiring") {
    val pluginClasspath = configurations.named("kotlinCompilerPluginClasspathMain")
    val runtimeClasspath = configurations.named("runtimeClasspath")
    doLast {
        println("E2E_PLUGIN_CLASSPATH=" + pluginClasspath.get().files.joinToString(",") { it.name })
        println("E2E_RUNTIME_CLASSPATH=" + runtimeClasspath.get().files.joinToString(",") { it.name })
    }
}
