plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    compileOnly(libs.slf4j)
    compileOnly(libs.minestom)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.bundles.log4j.runtime)

    implementation(projects.modules.twaddleLifecycle)
    implementation(projects.modules.twaddleStructuredLogging)
    implementation(projects.common.twaddleCommonMinestom)
    implementation(projects.modules.twaddleTransferRules)
    implementation(projects.modules.twaddleDataDriven)
}

sourceSets.named("main") {
    resources.srcDir(rootProject.file("text-fixtures/loot-tables/26.3"))
}
