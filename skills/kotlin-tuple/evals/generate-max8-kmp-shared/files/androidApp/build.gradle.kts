plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.acme.app.android"
    compileSdk = 36
}

dependencies {
    implementation(project(":shared"))
}
