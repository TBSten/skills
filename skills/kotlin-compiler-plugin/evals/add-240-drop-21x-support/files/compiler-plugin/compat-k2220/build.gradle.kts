plugins { kotlin("jvm") }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.20")
    implementation(project(":compiler-plugin:compat"))
    implementation(project(":compiler-plugin:compat-k210"))
}
