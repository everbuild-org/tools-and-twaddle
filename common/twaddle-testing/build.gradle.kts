plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Testing utils for Twaddle"

dependencies {
    implementation(libs.testballoon.lib)
    implementation(libs.testballoon.kotest)
    implementation(libs.minestom)
    api(libs.minestomTesting)
}

