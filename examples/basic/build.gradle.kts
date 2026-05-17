plugins {
    java
    id("org.openprojectx.gradle.mirror")
}

gradleMirror {
    configFile.set(layout.projectDirectory.file("gradle-mirror.yaml"))
    configureAllProjects.set(false)
}

tasks.register("printRepositories") {
    doLast {
        repositories.withType<org.gradle.api.artifacts.repositories.MavenArtifactRepository>().forEach { repository ->
            println("${repository.name}: ${repository.url}")
        }
    }
}
