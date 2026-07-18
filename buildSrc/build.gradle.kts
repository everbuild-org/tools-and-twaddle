plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.testballoon.plugin)
}
