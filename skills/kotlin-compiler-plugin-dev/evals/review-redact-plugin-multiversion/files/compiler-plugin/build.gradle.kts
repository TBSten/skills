plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "9.0.0"
}

val embedded by configurations.creating

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")
    implementation(project(":compat"))
    add(embedded.name, project(":compat-k220"))
    add(embedded.name, project(":compat-k230"))
    testImplementation("dev.zacsweers.kctfork:core:0.8.0")
}

tasks.shadowJar {
    configurations = listOf(embedded)
    archiveClassifier.set("")
}
