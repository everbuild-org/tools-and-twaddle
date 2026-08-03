package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    kotlin("jvm")
    id("de.infix.testBalloon")
    jacoco
}

val libs = versionCatalogs.named("libs")

kotlin {
    jvmToolchain(25)
}

jacoco {
    toolVersion = "0.8.14"
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("mockito-core").get())
    testImplementation(libs.findLibrary("mockito-kotlin").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testImplementation(libs.findLibrary("testballoon-lib").get())
    testImplementation(libs.findLibrary("testballoon-kotest").get())
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.named("jacocoTestReport"))

    useJUnitPlatform()

    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required = true
        html.required = true
    }
}
