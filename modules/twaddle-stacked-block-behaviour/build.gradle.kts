plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.minestom)
    implementation(projects.twaddleCore)
    implementation(projects.common.twaddleCommonBlock)
    implementation(projects.common.twaddleTesting)
}

