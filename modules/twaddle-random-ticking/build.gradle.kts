plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Random block ticking for Minestom instances"

dependencies {
    compileOnly(libs.minestom)
    api(projects.common.twaddleCommonBlock)
    testImplementation(projects.common.twaddleTesting)
}
