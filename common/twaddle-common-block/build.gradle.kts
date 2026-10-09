plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Common block API for Twaddle"

dependencies {
    compileOnly(libs.minestom)
    implementation(projects.twaddleCore)
}

