plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

val embedded by configurations.creating

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
    implementation(project(":compiler-plugin:compat"))
    add(embedded.name, project(":compiler-plugin:compat-k210"))
    add(embedded.name, project(":compiler-plugin:compat-k2220"))
    add(embedded.name, project(":compiler-plugin:compat-k230"))
    add(embedded.name, project(":compiler-plugin:compat-k240_beta2"))
}

tasks.shadowJar {
    configurations = listOf(embedded)
    mergeServiceFiles()
}
