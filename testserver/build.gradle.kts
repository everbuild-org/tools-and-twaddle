plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.bundles.log4j.runtime)

    implementation(projects.twaddleCore)
    implementation(projects.modules.twaddleTransferRules)
}