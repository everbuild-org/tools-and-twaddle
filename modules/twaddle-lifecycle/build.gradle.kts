plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Application lifetime and coroutine resource ownership"

dependencies {
    compileOnly(libs.minestom)
    api(libs.kotlinx.coroutines)
    implementation(projects.modules.twaddleStructuredLogging)
    testImplementation(libs.minestom)
}
