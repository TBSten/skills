plugins { kotlin("jvm") }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0-Beta2")
    implementation(project(":compiler-plugin:compat"))
    implementation(project(":compiler-plugin:compat-k230"))
}
