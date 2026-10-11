plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Common Minestom event and packet interoperability helpers"

dependencies {
    compileOnly(libs.minestom)
}
