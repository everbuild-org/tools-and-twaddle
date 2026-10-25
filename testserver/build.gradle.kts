import org.everbuild.twaddle.build.DownloadBlockLootTables

plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    compileOnly(libs.slf4j)
    implementation(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.bundles.log4j.runtime)

    implementation(projects.modules.twaddleLifecycle)
    implementation(projects.modules.twaddleStructuredLogging)
    implementation(projects.common.twaddleCommonMinestom)
    implementation(projects.modules.twaddleTransferRules)
    implementation(projects.modules.twaddleDataDriven)
    implementation(projects.modules.twaddleLoot)
}

val minecraftDataVersionProvider = providers.gradleProperty("minecraft.data.version")
val generatedBlockLootTables = layout.buildDirectory.dir("generated/resources/block-loot-tables")

val downloadBlockLootTables = tasks.register<DownloadBlockLootTables>("downloadBlockLootTables") {
    minecraftDataVersion.set(minecraftDataVersionProvider)
    aggregateFile.set(
        layout.buildDirectory.file(
            minecraftDataVersionProvider.map { version -> "downloads/minecraft/$version/block-loot-tables.json" },
        ),
    )
    generatedResourcesDirectory.set(generatedBlockLootTables)
}

sourceSets.named("main") {
    resources.srcDir(rootProject.file("text-fixtures/loot-tables/26.3"))
    resources.srcDir(generatedBlockLootTables)
}

tasks.named("processResources") {
    dependsOn(downloadBlockLootTables)
}

tasks.named("sourcesJar") {
    dependsOn(downloadBlockLootTables)
}
