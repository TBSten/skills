plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.kotlinGradlePlugin)
    implementation(project(":compiler-plugin"))
    implementation(project(":runtime"))
}

gradlePlugin {
    plugins {
        create("<plugin-short-name>") {
            id = "<your-plugin-id>"
            implementationClass = "<your-package>.gradle.<YourGradlePlugin>"
        }
    }
}
