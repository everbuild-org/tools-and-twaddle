plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Loot generation for Minestom servers"

dependencies {
    compileOnly(libs.minestom)
}
