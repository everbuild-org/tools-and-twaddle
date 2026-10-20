plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Data-loading and registration for Minestom"

dependencies {
    compileOnly(libs.minestom)
    api(libs.kotlinx.coroutines)
    testImplementation(libs.minestom)
}
