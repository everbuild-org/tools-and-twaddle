plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Inventory transfer rules for customizing shift-clicks"

dependencies {
    compileOnly(libs.minestom)
    implementation(projects.twaddleCore)
}

