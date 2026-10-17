plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Loot generation for Minestom servers"

dependencies {
    compileOnly(libs.minestom)
}

sourceSets.named("test") {
    resources.srcDir(rootProject.file("text-fixtures/loot-tables/26.3"))
}
