plugins {
    // @bootup:if kmp
    id("example-lib.kmp")
    // @bootup:end
    // @bootup:if jvm
    //~ id("example-lib.jvm")
    // @bootup:end
    id("example-lib.publish")
}

// @bootup:if kmp
kotlin {
    sourceSets {
        commonMain.dependencies {
            // 公開 API のシグネチャに現れる依存は api(...)、それ以外は implementation(...)
        }
    }
}
// @bootup:end
// @bootup:if jvm
//~ dependencies {
//~     // 公開 API のシグネチャに現れる依存は api(...)、それ以外は implementation(...)
//~ }
// @bootup:end
