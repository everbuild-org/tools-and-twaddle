plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.publishing")
}

description = "Reusable Minecraft datapack primitives, including number providers"

dependencies {
    compileOnly(libs.minestom)
    implementation(projects.modules.twaddleDataDriven)
    testImplementation(libs.minestom)
    testImplementation(projects.testing.twaddleDataDrivenTesting)
    testImplementation(projects.testing.twaddleTesting)
}

sourceSets.named("test") {
    resources.srcDir(rootProject.file("text-fixtures/datapack-primitives/26.3"))
}
