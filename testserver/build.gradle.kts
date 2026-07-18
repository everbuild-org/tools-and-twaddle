plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.tinylog.api)
    implementation(libs.tinylog.impl)
    implementation(libs.tinylog.slf4j)
    implementation(project(":twaddle-core"))
}