plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Test helpers for data-driven resources and codecs"

dependencies {
    api(libs.minestom)
    api(libs.testballoon.lib)
    api(projects.modules.twaddleDataDriven)
}
