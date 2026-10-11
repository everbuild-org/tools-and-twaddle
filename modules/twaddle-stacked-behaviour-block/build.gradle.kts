plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Block behaviour built on the stacked behaviour API"

dependencies {
    compileOnly(libs.minestom)
    api(projects.modules.twaddleStackedBehaviour)
    testImplementation(projects.common.twaddleTesting)
}
