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
        create("examplePlugin") {
            id = "com.example.compilerpluginsetup"
            implementationClass = "com.example.compilerpluginsetup.gradle.ExampleGradlePlugin"
        }
    }
}
