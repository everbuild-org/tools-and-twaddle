plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.bundles.log4j.runtime)
    implementation(project(":twaddle-core"))
    implementation(project(":modules:twaddle-transfer-rules"))
}