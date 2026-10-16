plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Block behaviour built on the stacked behaviour API"

dependencies {
    compileOnly(libs.minestom)
    api(projects.modules.twaddleStackedBehaviour)
    api(projects.common.twaddleCommonBlock)
    testImplementation(projects.testing.twaddleTesting)
}
