plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.slf4j)
    implementation(libs.kotlinx.coroutines)
}
