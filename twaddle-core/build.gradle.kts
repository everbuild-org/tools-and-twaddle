plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.minestom)
    implementation(libs.slf4j)
    api(libs.kotlinx.coroutines)
}
