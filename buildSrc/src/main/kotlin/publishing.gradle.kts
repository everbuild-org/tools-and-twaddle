package buildsrc.convention

import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
}

publishing {
    repositories {
        mavenLocal()
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