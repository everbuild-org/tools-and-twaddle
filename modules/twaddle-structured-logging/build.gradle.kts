plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Structured SLF4J logging for Kotlin and Java"

dependencies {
    api(libs.slf4j)
}
