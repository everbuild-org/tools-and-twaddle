plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Minestom-based rule stacking across activity domains"

dependencies {
    compileOnly(libs.minestom)
    testImplementation(projects.testing.twaddleTesting)
}
