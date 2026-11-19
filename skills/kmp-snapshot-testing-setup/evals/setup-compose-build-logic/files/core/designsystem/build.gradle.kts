plugins {
    id("convention-kmp")
    id("convention-compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
        }
    }
}
