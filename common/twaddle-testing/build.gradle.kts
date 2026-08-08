plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.testballoon.lib)
    implementation(libs.testballoon.kotest)
    implementation(libs.minestom)
    api(libs.minestomTesting)
}

