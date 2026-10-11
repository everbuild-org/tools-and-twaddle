package buildsrc.convention

import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "asordaPublic"
            url = uri("https://mvn.everbuild.org/public")
            credentials {
                username = providers.environmentVariable("MVN_TWADDLE_USER").orNull
                password = providers.environmentVariable("MVN_TWADDLE_PASSWORD").orNull
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/everbuild-org/tools-and-twaddle"

                licenses {
                    license {
                        name = "MIT"
                        url = "https://github.com/everbuild-org/tools-and-twaddle/blob/main/LICENSE"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/everbuild-org/tools-and-twaddle.git"
                    developerConnection = "scm:git:ssh://git@github.com/everbuild-org/tools-and-twaddle.git"
                    url = "https://github.com/everbuild-org/tools-and-twaddle"
                }
            }
        }
    }
}
