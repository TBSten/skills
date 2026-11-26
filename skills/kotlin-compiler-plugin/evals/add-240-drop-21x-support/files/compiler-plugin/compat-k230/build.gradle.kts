plugins { kotlin("jvm") }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")
    implementation(project(":compiler-plugin:compat"))
    implementation(project(":compiler-plugin:compat-k2220"))
}
