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
}
