dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core")
includeImmediateChildren("modules", "module")
include(":testserver")

fun includeImmediateChildren(path: String, prefix: String) {
    File(path).listFiles()?.forEach { file ->
        if (file.isDirectory) {
            include(":$path:${file.name}")
        }
    }
}

rootProject.name = "tools-and-twaddle"