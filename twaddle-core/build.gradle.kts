plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Shared utils & lifecycle management for Twaddle"

dependencies {
    compileOnly(libs.minestom)
    compileOnly(libs.slf4j)
    api(libs.kotlinx.coroutines)
}
