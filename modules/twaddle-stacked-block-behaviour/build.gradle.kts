plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Implement block behaviour once, stack on top of each other, and have them all be executed"

dependencies {
    implementation(libs.minestom)
    implementation(projects.twaddleCore)
    implementation(projects.common.twaddleCommonBlock)
    testImplementation(projects.common.twaddleTesting)
}

