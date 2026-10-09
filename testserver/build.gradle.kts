plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    compileOnly(libs.slf4j)
    compileOnly(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.bundles.log4j.runtime)

    implementation(projects.twaddleCore)
    implementation(projects.modules.twaddleTransferRules)
}