plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        mainRun {
            mainClass = "<your-package>.testapp.MainKt"
        }
    }
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
        }
    }
}

dependencies {
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}
