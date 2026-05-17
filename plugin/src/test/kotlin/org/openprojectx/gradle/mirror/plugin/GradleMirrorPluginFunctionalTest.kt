package org.openprojectx.gradle.mirror.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleMirrorPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `project plugin configures mirrored repositories from yaml`() {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "sample"""")
        projectDir.resolve("gradle-mirror.yaml").writeText(
            """
            repositories:
              - id: central
                url: https://repo.maven.apache.org/maven2
            mirrors:
              - id: company
                url: https://repo.example.com/maven
                mirrorOf: central
                credentials:
                  username: user
                  password: pass
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.gradle.mirror")
            }

            tasks.register("printRepos") {
                doLast {
                    repositories.forEach {
                        println("repo=" + it.name + ":" + (it as org.gradle.api.artifacts.repositories.MavenArtifactRepository).url)
                    }
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printRepos", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepos")?.outcome)
        assertTrue(result.output.contains("repo=company:https://repo.example.com/maven"))
    }

    @Test
    fun `project plugin resolves credentials from secrets properties file`() {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "sample"""")
        projectDir.resolve(".secrets.properties").writeText(
            """
            repo1_username=secret-user
            repo1_password=secret-password
            """.trimIndent(),
        )
        projectDir.resolve("gradle-mirror.yaml").writeText(
            """
            repositories:
              - id: central
                url: https://repo.maven.apache.org/maven2
            mirrors:
              - id: company
                url: https://repo.example.com/maven
                mirrorOf: central
                credentials:
                  username: ${'$'}{repo1_username}
                  password: ${'$'}{repo1_password}
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.gradle.mirror")
            }

            tasks.register("printCredentials") {
                doLast {
                    val repository = repositories.getByName("company") as org.gradle.api.artifacts.repositories.MavenArtifactRepository
                    val credentials = repository.credentials as org.gradle.api.artifacts.repositories.PasswordCredentials
                    println("username=" + credentials.username)
                    println("password=" + credentials.password)
                    println("extra=" + project.extensions.extraProperties["repo1_password"])
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("printCredentials", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printCredentials")?.outcome)
        assertTrue(result.output.contains("username=secret-user"))
        assertTrue(result.output.contains("password=secret-password"))
        assertTrue(result.output.contains("extra=secret-password"))
    }
}
