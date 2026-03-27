plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":runtime"))
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}

application {
    mainClass = "<your-package>.testapp.MainKt"
}
