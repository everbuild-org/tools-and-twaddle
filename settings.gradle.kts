dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeImmediateChildren("common")
includeImmediateChildren("modules")
includeImmediateChildren("testing")
include(":testserver")

fun includeImmediateChildren(path: String) {
    File(path)
        .listFiles()
        ?.filter { it.isDirectory && File(it, "build.gradle.kts").isFile }
        ?.forEach { file -> include(":$path:${file.name}") }
}

rootProject.name = "tools-and-twaddle"
