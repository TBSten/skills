plugins {
    id("convention-kmp")
    id("convention-compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
