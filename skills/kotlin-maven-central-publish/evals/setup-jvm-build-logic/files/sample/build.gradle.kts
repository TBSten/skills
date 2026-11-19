plugins {
    id("kotlin-jvm-convention")
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("MainKt")
}
